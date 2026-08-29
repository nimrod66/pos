#Requires -Version 5.1
# brutal-authz.ps1 — Authorization torture tests.
# Sources brutal-common.ps1 for shared utilities.
. "$PSScriptRoot\brutal-common.ps1"

Write-Host ""
Write-Host "========================================================" -ForegroundColor White
Write-Host "  BRUTAL AUTHORIZATION TORTURE TESTS" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor White

# ─────────────────────────────────────────────────────────
# Authz1: Cross-Tenant Isolation
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Authz1-CrossTenantIsolation"

$pharmacyA = New-Login "admin@demo.com" "admin123"
if (-not $pharmacyA.session) {
    Record "Authz1-Login-PharmacyA" "BLOCKED" "Could not login as Pharmacy A owner"
    End-TestGroup
} else {
    Record "Authz1-Login-PharmacyA" "PASS" ""
    $pharmacyAId = $pharmacyA.pharmacyId
    $pharmacyABranchId = $pharmacyA.branchId

    $pharmacyBBranchId = Db-Scalar "SELECT id FROM branch WHERE pharmacy_id != '$pharmacyAId' LIMIT 1"
    if (-not $pharmacyBBranchId) {
        Record "Authz1-Find-PharmacyB" "BLOCKED" "No second pharmacy found in DB"
    } else {
        Record "Authz1-Find-PharmacyB" "PASS" ""

        $r = Call "GET" "/branches?pharmacyId=$pharmacyBBranchId" $pharmacyA
        $branchLeak = $false
        if ($r.ok -and $r.data) {
            $branchCount = @($r.data).Count
            if ($branchCount -gt 0) {
                $branchLeak = $true
            }
        }
        Assert "Authz1-Branches-Blocked" (-not $branchLeak) "Cross-tenant branch access should be denied or return empty"

        $r = Call "GET" "/sales?branchId=$pharmacyBBranchId&size=100" $pharmacyA
        $salesLeak = $false
        if ($r.ok -and $r.data) {
            $salesCount = @($r.data.content).Count
            if ($salesCount -gt 0) {
                $salesLeak = $true
            }
        }
        Assert "Authz1-Sales-Blocked" (-not $salesLeak) "Cross-tenant sales access should be denied or return empty"

        $r = Call "GET" "/customers?branchId=$pharmacyBBranchId&size=100" $pharmacyA
        $custLeak = $false
        if ($r.ok -and $r.data) {
            $custCount = @($r.data.content).Count
            if ($custCount -gt 0) {
                $custLeak = $true
            }
        }
        Assert "Authz1-Customers-Blocked" (-not $custLeak) "Cross-tenant customer access should be denied or return empty"

        $dbSalesLeak = Db-Scalar "SELECT COUNT(*) FROM sales WHERE branch_id = '$pharmacyBBranchId' AND status = 'COMPLETED'"
        $apiSalesCount = 0
        if ($r.ok -and $r.data) { $apiSalesCount = @($r.data.content).Count }
        Assert "Authz1-DB-Consistent" ($dbSalesLeak -eq 0 -or $apiSalesCount -eq 0) "DB verification: no data leaked across tenants"
    }
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Authz2: Comprehensive Role Boundaries
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Authz2-RoleBoundaries"

$roleTests = @(
    @{ role = "CASHIER"; email = "cashier@demo.com"; password = "cashier123"; endpoints = @(
        @{ method = "GET"; path = "/users"; name = "ListUsers" },
        @{ method = "GET"; path = "/system-settings"; name = "ViewSettings" },
        @{ method = "GET"; path = "/audit-logs"; name = "ViewAudit" },
        @{ method = "POST"; path = "/users"; name = "CreateUser"; body = @{ email = "hack@test.com"; password = "test123"; firstName = "H"; lastName = "Attacker" } }
    )},
    @{ role = "STOREKEEPER"; email = "storekeeper@demo.com"; password = "stock1234"; endpoints = @(
        @{ method = "GET"; path = "/expenses"; name = "ListExpenses" },
        @{ method = "POST"; path = "/expenses"; name = "CreateExpense"; body = @{ amount = 5000; description = "Fake" } },
        @{ method = "GET"; path = "/cash-transactions"; name = "ListCashDrawers" }
    )},
    @{ role = "PHARMACIST"; email = "pharmacist@demo.com"; password = "pharmacist123"; endpoints = @(
        @{ method = "GET"; path = "/users"; name = "ListUsers" },
        @{ method = "POST"; path = "/users"; name = "CreateUser"; body = @{ email = "hack2@test.com"; password = "test123"; firstName = "H"; lastName = "Attacker" } }
    )},
    @{ role = "MANAGER"; email = "manager@demo.com"; password = "manager123"; endpoints = @(
        @{ method = "POST"; path = "/users"; name = "CreateUser"; body = @{ email = "hack3@test.com"; password = "test123"; firstName = "H"; lastName = "Attacker" } }
    )}
)

foreach ($rt in $roleTests) {
    $userCtx = New-Login $rt.email $rt.password
    if (-not $userCtx.session) {
        Record "Authz2-$($rt.role)-Login" "BLOCKED" "Could not login as $($rt.role)"
        continue
    }
    Record "Authz2-$($rt.role)-Login" "PASS" ""

    foreach ($ep in $rt.endpoints) {
        $r = Call $ep.method $ep.path $userCtx $ep.body
        $denied = (-not $r.ok) -and ($r.status -eq 403 -or $r.status -eq 401)
        Assert "Authz2-$($rt.role)-$($ep.name)-Denied" $denied "Expected 403 but got status=$($r.status)"
    }
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Authz3: Session Management
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Authz3-SessionManagement"

$sessionUser = "cashier@demo.com"
$sessionPass = "cashier123"

$session1 = New-Login $sessionUser $sessionPass
if (-not $session1.session) {
    Record "Authz3-First-Login" "BLOCKED" "Could not create first session"
    End-TestGroup
} else {
    Record "Authz3-First-Login" "PASS" ""

    $r1 = Call "GET" "/auth/me" $session1
    Assert "Authz3-First-Session-Valid" $r1.ok "First session should be valid"

    $session2 = New-Login $sessionUser $sessionPass
    if (-not $session2.session) {
        Record "Authz3-Second-Login" "BLOCKED" "Could not create second session"
    } else {
        Record "Authz3-Second-Login" "PASS" ""

        $r2 = Call "GET" "/auth/me" $session2
        Assert "Authz3-Second-Session-Valid" $r2.ok "Second session should be valid"

        $rOld = Call "GET" "/auth/me" $session1
        $oldSessionInvalid = (-not $rOld.ok) -and ($rOld.status -eq 401)
        Assert "Authz3-Old-Session-Evicted" $oldSessionInvalid "Old session should be 401 after re-login"

        $activeSessions = Db-Scalar "SELECT COUNT(*) FROM spring_sessions WHERE principal_name = '$sessionUser'"
        Assert "Authz3-Single-Active-Session" ($activeSessions -le 1) "Only one active session expected, got $activeSessions"
    }
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Authz4: CSRF Protection
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Authz4-CSRFProtection"

$csrfCtx = New-Login "admin@demo.com" "admin123"
if (-not $csrfCtx.session) {
    Record "Authz4-Login" "BLOCKED" "Could not login for CSRF tests"
    End-TestGroup
} else {
    Record "Authz4-Login" "PASS" ""

    $noCsrfCtx = @{
        session = $csrfCtx.session
        headers = @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }
    }

    $postResult = Call "POST" "/cash-transactions" $noCsrfCtx @{ transactionType = "CASH_IN"; amount = 100; remarks = "CSRF test" }
    $csrfBlocked = (-not $postResult.ok) -and ($postResult.status -eq 403)
    if (-not $csrfBlocked) {
        $csrfBlocked = $postResult.error -match "CSRF"
    }
    Assert "Authz4-POST-Without-CSRF-Blocked" $csrfBlocked "POST without CSRF token should be 403"

    $getResult = Call "GET" "/medicines?size=1" $csrfCtx
    Assert "Authz4-GET-With-CSRF-Succeeds" $getResult.ok "GET with CSRF should succeed"
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Output and summary
# ─────────────────────────────────────────────────────────
Write-Summary

$reportPath = Join-Path $PSScriptRoot "brutal-authz-results.json"
Get-TestReport | Out-File -FilePath $reportPath -Encoding UTF8
Write-Host "`nResults written to: $reportPath" -ForegroundColor DarkGray

if ($script:FailCount -gt 0) { exit 1 }
