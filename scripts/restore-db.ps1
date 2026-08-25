# Restores a backup produced by backup-db.ps1 into the running stack.
# Stops the API during the restore, verifies the result, restarts it.
#
# Usage:
#   powershell -File scripts\restore-db.ps1 -File ..\backups\pharmacy_pos_20260826_010000.dump
#   powershell -File scripts\restore-db.ps1 -File x.dump -Force   # skip confirmation
param(
    [Parameter(Mandatory = $true)]
    [string]$File,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $File)) { throw "Backup file not found: $File" }

Write-Host "This will REPLACE the current pharmacy_pos database with:"
Write-Host "  $File"
if (-not $Force) {
    $answer = Read-Host "Type RESTORE to continue"
    if ($answer -ne "RESTORE") { Write-Host "Aborted."; exit 1 }
}

Write-Host "Stopping API..."
docker stop pharmacy-pos-pilot-api-1 | Out-Null

Write-Host "Restoring (this can take a while on large databases)..."
# Recreate schema inside a scratch DB first to validate the dump before touching real data.
docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "DROP DATABASE IF EXISTS restore_check;" 2>$null | Out-Null
docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "CREATE DATABASE restore_check;" | Out-Null
$containerFile = "/tmp/restore.dump"
docker cp $File "pharmacy-pos-pilot-postgres-1:$containerFile"
docker exec pharmacy-pos-pilot-postgres-1 pg_restore -U pharmacy_pos -d restore_check --no-owner --role=pharmacy_pos "$containerFile" 2>$null
if ($LASTEXITCODE -ne 0) {
    docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "DROP DATABASE IF EXISTS restore_check;" | Out-Null
    docker exec pharmacy-pos-pilot-postgres-1 rm "$containerFile"
    docker start pharmacy-pos-pilot-api-1 | Out-Null
    throw "Backup file failed validation against scratch database; live data untouched."
}

Write-Host "Dump validated. Applying to live database..."
docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='pharmacy_pos' AND pid <> pg_backend_pid();" | Out-Null
docker exec pharmacy-pos-pilot-postgres-1 dropdb -U pharmacy_pos --if-exists pharmacy_pos
docker exec pharmacy-pos-pilot-postgres-1 createdb -U pharmacy_pos --owner=pharmacy_pos pharmacy_pos
docker exec pharmacy-pos-pilot-postgres-1 pg_restore -U pharmacy_pos -d pharmacy_pos --no-owner --role=pharmacy_pos "$containerFile"
if ($LASTEXITCODE -ne 0) { throw "pg_restore into pharmacy_pos failed - inspect manually." }
docker exec pharmacy-pos-pilot-postgres-1 rm "$containerFile"

Write-Host "Restarting API..."
docker start pharmacy-pos-pilot-api-1 | Out-Null
Start-Sleep -Seconds 25
$health = Invoke-WebRequest -Uri "http://localhost:9090/actuator/health" -UseBasicParsing
Write-Host "API health: $($health.StatusCode) $($health.Content)"
Write-Host "Restore complete."
