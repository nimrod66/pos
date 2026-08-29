#Requires -Version 5.1
# brutal-lan.ps1 — LAN security boundary tests.
# Tests unauthenticated access, actuator exposure, terminal registration,
# backup endpoints, and SQL injection resilience.
# Sources brutal-common.ps1 for shared utilities.
. "$PSScriptRoot\brutal-common.ps1"

Write-Host ""
Write-Host "========================================================" -ForegroundColor White
Write-Host "  BRUTAL LAN SECURITY TESTS" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor White

# ─────────────────────────────────────────────────────────
# Helper: raw unauthenticated request (no session, no CSRF)
# ─────────────────────────────────────────────────────────

function Call-Unauth($method, $path, $body) {
    $params = @{
        Uri      = "$ApiBase$path"
        Method   = $method
        Headers  = @{}
    }
    if ($body) {
        $params.ContentType = "application/json"
        if ($body -is [string]) { $params.Body = $body } else { $params.Body = ($body | ConvertTo-Json -Depth 10) }
    }
    try {
        $resp = Invoke-RestMethod @params
        return @{ ok = $true; data = $resp; status = 200; error = $null }
    } catch {
        $statusCode = 0
        try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
        $msg = ""
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $msg = $sr.ReadToEnd()
        } catch { $msg = $_.Exception.Message }
        return @{ ok = $false; data = $null; status = $statusCode; error = $msg }
    }
}

# ============================================================
# Lan1: Unauthenticated API Access
# ============================================================
Begin-TestGroup "Lan1-UnauthenticatedAPIAccess"

$r = Call-Unauth "GET" "/api/v1/users"
Assert-Status "Lan1-GET-users-denied" $r 401

$r = Call-Unauth "GET" "/api/v1/sales"
Assert-Status "Lan1-GET-sales-denied" $r 401

$r = Call-Unauth "GET" "/api/v1/customers"
Assert-Status "Lan1-GET-customers-denied" $r 401

    $r = Call-Unauth "POST" "/api/v1/sales" @{ clientSaleId = "lan-test"; items = @(); payments = @() }
    Assert "Lan1-POST-sales-denied" ((-not $r.ok) -and ($r.status -eq 401 -or $r.status -eq 403)) "Expected 401/403 but got status=$($r.status)"

End-TestGroup

# ============================================================
# Lan2: Actuator Endpoint Protection
# ============================================================
Begin-TestGroup "Lan2-ActuatorEndpointProtection"

$health = Call-Unauth "GET" "/actuator/health"
if ($health.ok -and $health.status -eq 200) {
    Record "Lan2-actuator-health" "PASS" "Health endpoint is publicly accessible (expected for health checks)"
} elseif ($health.status -eq 401 -or $health.status -eq 403) {
    Record "Lan2-actuator-health" "PASS" "Health endpoint is protected (status=$($health.status))"
} else {
    Record "Lan2-actuator-health" "BLOCKED" "Unexpected status=$($health.status)"
}

$env = Call-Unauth "GET" "/actuator/env"
Assert "Lan2-actuator-env-protected" ((-not $env.ok) -and ($env.status -eq 401 -or $env.status -eq 403)) "Expected 401/403 but got status=$($env.status)"

$beans = Call-Unauth "GET" "/actuator/beans"
Assert "Lan2-actuator-beans-protected" ((-not $beans.ok) -and ($beans.status -eq 401 -or $beans.status -eq 403)) "Expected 401/403 but got status=$($beans.status)"

$configprops = Call-Unauth "GET" "/actuator/configprops"
Assert "Lan2-actuator-configprops-protected" ((-not $configprops.ok) -and ($configprops.status -eq 401 -or $configprops.status -eq 403)) "Expected 401/403 but got status=$($configprops.status)"

End-TestGroup

# ============================================================
# Lan3: Terminal Registration Without Auth
# ============================================================
Begin-TestGroup "Lan3-TerminalRegistrationWithoutAuth"

    $r = Call-Unauth "POST" "/api/v1/terminals/register" @{ terminalName = "HACKED"; branchId = "fake" }
    Assert "Lan3-register-denied" ((-not $r.ok) -and ($r.status -eq 401 -or $r.status -eq 403)) "Expected 401/403 but got status=$($r.status)"

    $r = Call-Unauth "POST" "/api/v1/terminals/pair" @{ pairingCode = "HACKED123" }
    Assert "Lan3-pair-denied" ((-not $r.ok) -and ($r.status -eq 401 -or $r.status -eq 403)) "Expected 401/403 but got status=$($r.status)"

End-TestGroup

# ============================================================
# Lan4: Backup Endpoint Protection
# ============================================================
Begin-TestGroup "Lan4-BackupEndpointProtection"

$r = Call-Unauth "GET" "/api/v1/system/backup/list"
Assert-Status "Lan4-backup-list-denied" $r 401

    $r = Call-Unauth "POST" "/api/v1/system/backup" @{ type = "FULL" }
    Assert "Lan4-backup-create-denied" ((-not $r.ok) -and ($r.status -eq 401 -or $r.status -eq 403)) "Expected 401/403 but got status=$($r.status)"

End-TestGroup

# ============================================================
# Lan5: SQL Injection Attempts
# ============================================================
Begin-TestGroup "Lan5-SQLInjectionAttempts"

# SQLi via medicine search query parameter
$sqliSearch = Call-Unauth "GET" "/api/v1/medicines?search=%27+OR+1%3D1+--"
# Should return 401 (unauthenticated) or a normal empty/error response — NOT a SQL error
$isSqlError = $false
if ($sqliSearch.error -match "SQL|syntax|exception|pg_query|PSQLException|JDBC|ORA-|mysql|sqlite") {
    $isSqlError = $true
}
Assert "Lan5-search-sqli-no-sql-error" (-not $isSqlError) "SQL error detected in medicine search response: $($sqliSearch.error)"

# SQLi via login body
$sqliLogin = Call-Unauth "POST" "/api/v1/auth/login" @{ email = "admin'--"; password = "anything" }
$isSqlError2 = $false
if ($sqliLogin.error -match "SQL|syntax|exception|pg_query|PSQLException|JDBC|ORA-|mysql|sqlite") {
    $isSqlError2 = $true
}
Assert "Lan5-login-sqli-no-sql-error" (-not $isSqlError2) "SQL error detected in login response: $($sqliLogin.error)"
# Login should fail with 401 or similar, not 500
Assert "Lan5-login-sqli-not-500" ($sqliLogin.status -ne 500) "SQL injection caused 500 server error (status=$($sqliLogin.status))"

End-TestGroup

# ─────────────────────────────────────────────────────────
Write-Summary

$reportPath = Join-Path $PSScriptRoot "brutal-lan-results.json"
Get-TestReport | Out-File -FilePath $reportPath -Encoding UTF8
Write-Host "`nResults written to: $reportPath" -ForegroundColor DarkGray

if ($script:FailCount -gt 0) { exit 1 }
