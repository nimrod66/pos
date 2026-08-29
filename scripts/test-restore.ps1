$ErrorActionPreference = "Continue"
$ApiBase = "http://localhost:9090"

function New-Login($email, $pass) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $c = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
        -Body ('{"email":"' + $email + '","password":"' + $pass + '"}') `
        -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName = $c.token } | Out-Null
    $c2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    return @{ session = $s; headers = @{ $c2.headerName = $c2.token } }
}

# 1. Create backup via API
Write-Host "=== Step 1: Create backup ==="
$ctx = New-Login "admin@demo.com" "admin123"
$backupResp = Invoke-RestMethod -Uri "$ApiBase/api/v1/system/backup" -Method POST -WebSession $ctx.session -Headers $ctx.headers
$backupFile = $backupResp.data.filename
Write-Host "OK: $backupFile ($($backupResp.data.sizeBytes) bytes)"

# 2. Download and restore via docker exec
Write-Host "`n=== Step 2: Restore ==="
$tempFile = Join-Path $env:TEMP "restore_test.dump"
Invoke-WebRequest -Uri "$ApiBase/api/v1/system/backup/download/$backupFile" -OutFile $tempFile -WebSession $ctx.session -Headers $ctx.headers
docker cp $tempFile "pharmacy-pos-pilot-postgres-1:/tmp/restore_test.dump"

docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='pharmacy_pos' AND pid <> pg_backend_pid();" 2>$null | Out-Null
docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "DROP DATABASE IF EXISTS pharmacy_pos;" 2>$null | Out-Null
docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "CREATE DATABASE pharmacy_pos OWNER pharmacy_pos;" | Out-Null
docker exec pharmacy-pos-pilot-postgres-1 pg_restore --no-owner --clean --if-exists --role=pharmacy_pos -U pharmacy_pos -d pharmacy_pos /tmp/restore_test.dump 2>$null
Write-Host "pg_restore exit: $LASTEXITCODE"
docker exec pharmacy-pos-pilot-postgres-1 rm /tmp/restore_test.dump 2>$null | Out-Null

# 3. Restart API
Write-Host "`n=== Step 3: Restart API ==="
docker restart pharmacy-pos-pilot-api-1 | Out-Null
Write-Host "Waiting for API..."
Start-Sleep -Seconds 30
try {
    $health = Invoke-WebRequest -Uri "$ApiBase/actuator/health" -UseBasicParsing -TimeoutSec 10
    Write-Host "API health: $($health.StatusCode)"
} catch {
    Write-Host "API not ready, waiting more..."
    Start-Sleep -Seconds 15
}

# 4. Verify data
Write-Host "`n=== Step 4: Verify ==="
$ctx2 = New-Login "admin@demo.com" "admin123"
$me = Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/me" -WebSession $ctx2.session -Headers $ctx2.headers
Write-Host "Login: $($me.data.user.email)"

$meds = Invoke-RestMethod -Uri "$ApiBase/api/v1/medicines" -WebSession $ctx2.session -Headers $ctx2.headers
Write-Host "Medicines: $($meds.data.Count)"

# 5. Run smoke test
Write-Host "`n=== Step 5: Smoke test ==="
& .\scripts\smoke-test.ps1

Remove-Item $tempFile -ErrorAction SilentlyContinue
