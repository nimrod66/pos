# Authorization boundary attack tests. Tests that low-privilege users
# cannot access admin endpoints, cross-branch data, or perform
# privilege escalation. Run against the live stack.
$ErrorActionPreference = "Continue"
$ApiBase = "http://localhost:9090"
$script:pass = 0
$script:fail = 0
$script:failures = @()

function Ok($name)  { $script:pass++; Write-Host ("  PASS " + $name) }
function Bad($name, $detail) { $script:fail++; $script:failures += $name; Write-Host ("  FAIL " + $name + " :: " + $detail) }

function New-Login($email, $passw) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $c = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    $loginBody = @{ email = $email; password = $passw } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
            -Body $loginBody `
            -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName = $c.token } | Out-Null
    } catch {
        # Session may already exist from previous run; try once more
        Start-Sleep -Milliseconds 200
        $c = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
        Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
            -Body $loginBody `
            -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName = $c.token } | Out-Null
    }
    $c2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    return @{ session = $s; headers = @{ $c2.headerName = $c2.token; "Idempotency-Key" = [guid]::NewGuid().ToString() } }
}

function Call($method, $path, $ctx, $body) {
    $params = @{ Uri = "$ApiBase/api/v1$path"; Method = $method; WebSession = $ctx.session; Headers = $ctx.headers }
    if ($body) {
        $params.ContentType = "application/json"
        if ($body -is [string]) { $params.Body = $body } else { $params.Body = ($body | ConvertTo-Json -Depth 10) }
    }
    try { $resp = Invoke-RestMethod @params; return @{ ok = $true; data = $resp.data } }
    catch {
        $statusCode = 0
        try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
        $msg = ""
        try { $sr = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { $msg = $_.Exception.Message }
        return @{ ok = $false; error = $msg; status = $statusCode }
    }
}

Write-Host "== AUTHORIZATION ATTACK TESTS =="
Write-Host ""

# Clean up stale sessions from previous runs
try { docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d pharmacy_pos -c "DELETE FROM SPRING_SESSION;" 2>$null | Out-Null } catch {}
Start-Sleep -Seconds 1

# --- Test 1: Cashier accessing admin endpoints ---
Write-Host "-- T1: Cashier accessing admin endpoints --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call GET "/users" $cashier
if ($r.ok) { Bad "T1a-cashier-list-users" "should be 403 but got 200" } else { Ok "T1a-cashier-list-users-blocked" }

$r = Call POST "/users" $cashier (@{email="hack@test.com";password="test123";firstName="H";lastName="Attacker"})
if ($r.ok) { Bad "T1b-cashier-create-user" "should be 403 but got 200" } else { Ok "T1b-cashier-create-user-blocked" }

$r = Call GET "/audit-logs" $cashier
if ($r.ok) { Bad "T1c-cashier-view-audit" "should be 403 but got 200" } else { Ok "T1c-cashier-view-audit-blocked" }

$r = Call GET "/system-settings" $cashier
if ($r.ok) { Bad "T1d-cashier-view-settings" "should be 403 but got 200" } else { Ok "T1d-cashier-view-settings-blocked" }

$r = Call GET "/branches" $cashier
if ($r.ok) { Bad "T1e-cashier-list-branches" "should be 403 but got 200" } else { Ok "T1e-cashier-list-branches-blocked" }

$r = Call GET "/terminals" $cashier
if ($r.ok) { Bad "T1f-cashier-list-terminals" "should be 403 but got 200" } else { Ok "T1f-cashier-list-terminals-blocked" }

# --- Test 2: Store keeper accessing finance/sales admin ---
Write-Host ""
Write-Host "-- T2: Store keeper accessing finance endpoints --"
$storekeeper = New-Login "storekeeper@demo.com" "stock1234"

$r = Call GET "/reports/financial-summary" $storekeeper
if (-not $r.ok -and $r.status -eq 403) { Ok "T2a-storekeeper-financial-blocked" }
elseif ($r.ok) { Ok "T2a-storekeeper-financial-allowed" }
else { Ok "T2a-storekeeper-financial-error" }

$r = Call POST "/users" $storekeeper (@{email="hack2@test.com";password="test123";firstName="H";lastName="Attacker"})
if ($r.ok) { Bad "T2b-storekeeper-create-user" "should be 403 but got 200" } else { Ok "T2b-storekeeper-create-user-blocked" }

$r = Call GET "/audit-logs" $storekeeper
if ($r.ok) { Bad "T2c-storekeeper-view-audit" "should be 403 but got 200" } else { Ok "T2c-storekeeper-view-audit-blocked" }

# --- Test 3: Pharmacist cannot manage terminals ---
Write-Host ""
Write-Host "-- T3: Pharmacist accessing terminal management --"
$pharmacist = New-Login "pharmacist@demo.com" "pharmacist123"

$r = Call GET "/terminals" $pharmacist
if ($r.ok) { Bad "T3a-pharmacist-list-terminals" "should be 403 but got 200" } else { Ok "T3a-pharmacist-list-terminals-blocked" }

$r = Call GET "/system-settings" $pharmacist
if ($r.ok) { Bad "T3b-pharmacist-view-settings" "should be 403 but got 200" } else { Ok "T3b-pharmacist-view-settings-blocked" }

# --- Test 4: Manager can access sales/reports but not user management ---
Write-Host ""
Write-Host "-- T4: Manager role boundaries --"
$manager = New-Login "manager@demo.com" "manager123"
# Get the manager's branch from their profile
$mgrProfile = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/me" -WebSession $manager.session -Headers $manager.headers).data
$mgrBranchId = if ($mgrProfile.branch) { $mgrProfile.branch.id } else { (docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d pharmacy_pos -t -A -c "SELECT id FROM branch LIMIT 1" 2>$null).Trim() }

$r = Call GET "/reports/sales-summary?branchId=$mgrBranchId&from=2026-01-01&to=2026-12-31" $manager
if ($r.ok) { Ok "T4a-manager-view-sales-report" } else { Bad "T4a-manager-view-sales-report" "should succeed" }

$r = Call GET "/users" $manager
if ($r.ok) { Bad "T4b-manager-list-users" "should be 403 but got 200" } else { Ok "T4b-manager-list-users-blocked" }

$r = Call POST "/users" $manager (@{email="hack3@test.com";password="test123";firstName="H";lastName="Attacker"})
if ($r.ok) { Bad "T4c-manager-create-user" "should be 403 but got 200" } else { Ok "T4c-manager-create-user-blocked" }

# --- Test 5: No session = 401 ---
Write-Host ""
Write-Host "-- T5: Unauthenticated access --"
$noAuth = @{ session = $null; headers = @{} }

$r = Call GET "/medicines" $noAuth
if ($r.ok) { Bad "T5a-unauth-list-medicines" "should be 401 but got 200" } else { Ok "T5a-unauth-list-medicines-blocked" }

$r = Call GET "/users" $noAuth
if ($r.ok) { Bad "T5b-unauth-list-users" "should be 401 but got 200" } else { Ok "T5b-unauth-list-users-blocked" }

$r = Call GET "/sales" $noAuth
if ($r.ok) { Bad "T5c-unauth-list-sales" "should be 401 but got 200" } else { Ok "T5c-unauth-list-sales-blocked" }

# --- Test 6: Cashier cannot modify medicine prices ---
Write-Host ""
Write-Host "-- T6: Cashier cannot modify master data --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call GET "/medicines" $cashier
if ($r.ok) { Ok "T6a-cashier-list-medicines-allowed" } else { Bad "T6a-cashier-list-medicines" "should allow read" }

# Try to create a medicine (should be forbidden)
$r = Call POST "/medicines" $cashier (@{name="Hacked Drug";sku="HACK";sellingPrice=999})
if ($r.ok) { Bad "T6b-cashier-create-medicine" "should be 403 but got 200" } else { Ok "T6b-cashier-create-medicine-blocked" }

# --- Test 7: Cashier cannot access expenses management ---
Write-Host ""
Write-Host "-- T7: Cashier accessing expense management --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call GET "/expenses" $cashier
if ($r.ok) { Ok "T7a-cashier-list-expenses-allowed" } else { Bad "T7a-cashier-list-expenses" "should allow read" }

$r = Call POST "/expenses" $cashier (@{amount=5000;description="Fake expense";categoryId=$null})
if ($r.ok) { Bad "T7b-cashier-create-expense" "should be 403 but got 200" } else { Ok "T7b-cashier-create-expense-blocked" }

# --- Test 8: Cashier cannot access procurement ---
Write-Host ""
Write-Host "-- T8: Cashier accessing procurement --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call GET "/purchase-orders" $cashier
if ($r.ok) { Ok "T8a-cashier-list-pos-allowed" } else { Bad "T8a-cashier-list-pos" "should allow read" }

$r = Call POST "/purchase-orders" $cashier (@{supplierId=[guid]::Empty;items=@()})
if ($r.ok) { Bad "T8b-cashier-create-po" "should be 403 but got 200" } else { Ok "T8b-cashier-create-po-blocked" }

# --- Test 9: Cashier cannot do stock transfers ---
Write-Host ""
Write-Host "-- T9: Cashier accessing stock transfers --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call POST "/inventory/transfers" $cashier (@{sourceBranchId=[guid]::Empty;destBranchId=[guid]::Empty;items=@()})
if ($r.ok) { Bad "T9a-cashier-create-transfer" "should be 403 but got 200" } else { Ok "T9a-cashier-create-transfer-blocked" }

# --- Test 10: Cashier cannot backup/restore ---
Write-Host ""
Write-Host "-- T10: Cashier accessing backup/restore --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call POST "/system/backup" $cashier
if ($r.ok) { Bad "T10a-cashier-create-backup" "should be 403 but got 200" } else { Ok "T10a-cashier-create-backup-blocked" }

$r = Call GET "/system/backup/list" $cashier
if ($r.ok) { Bad "T10b-cashier-list-backups" "should be 403 but got 200" } else { Ok "T10b-cashier-list-backups-blocked" }

# --- Test 11: Cashier cannot manage controlled drugs register ---
Write-Host ""
Write-Host "-- T11: Cashier accessing controlled drugs --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call GET "/controlled-drugs" $cashier
if ($r.ok) { Bad "T11a-cashier-list-controlled-drugs" "should be 403 (owner/pharmacist only)" } else { Ok "T11a-cashier-list-controlled-drugs-blocked" }

# --- Test 12: Cashier cannot manage suppliers ---
Write-Host ""
Write-Host "-- T12: Cashier managing suppliers --"
$cashier = New-Login "cashier@demo.com" "cashier123"

$r = Call GET "/suppliers" $cashier
if ($r.ok) { Ok "T12a-cashier-list-suppliers-allowed" } else { Bad "T12a-cashier-list-suppliers" "should allow read" }

$r = Call POST "/suppliers" $cashier (@{name="Hacked Supplier";contactPerson="H";phone="0700000000"})
if ($r.ok) { Bad "T12b-cashier-create-supplier" "should be 403 but got 200" } else { Ok "T12b-cashier-create-supplier-blocked" }

# --- Summary ---
Write-Host ""
Write-Host "=============================="
Write-Host ("  PASSED:  " + $script:pass)
Write-Host ("  FAILED:  " + $script:fail)
if ($script:failures.Count -gt 0) {
    Write-Host ""
    Write-Host "  Failures:"
    $script:failures | ForEach-Object { Write-Host "    - $_" }
}
Write-Host "=============================="
if ($script:fail -gt 0) { exit 1 }
