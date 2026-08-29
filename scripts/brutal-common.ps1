#Requires -Version 5.1
# brutal-common.ps1 — Shared library for the Pharmacy POS brutal test suite.
# Dot-source this file from any brutal test script:
#   . (Join-Path $PSScriptRoot "brutal-common.ps1")

$ErrorActionPreference = "Continue"
$ApiBase = "http://localhost:9090"

$script:BrutalResults = [System.Collections.ArrayList]::new()
$script:TestGroups = [System.Collections.Stack]::new()
$script:GroupStats = [System.Collections.ArrayList]::new()
$script:PassCount = 0
$script:FailCount = 0
$script:BlockedCount = 0

$script:ContainerName = "pharmacy-pos-pilot-api-1"
$script:DbContainer = "pharmacy-pos-pilot-postgres-1"
$script:DbUser = "pharmacy_pos"
$script:DbName = "pharmacy_pos"
$script:DockerComposeDir = (Join-Path $PSScriptRoot "..")

# ─────────────────────────────────────────────────────────
# Utility helpers
# ─────────────────────────────────────────────────────────

function Get-UniqueId { return [guid]::NewGuid().ToString().Substring(0, 8) }

function Timestamp { return (Get-Date -Format "HH:mm:ss.fff") }

function Write-Ts($color, $msg) {
    $ts = Timestamp
    Write-Host "[$ts] " -ForegroundColor DarkGray -NoNewline
    Write-Host $msg -ForegroundColor $color
}

function Record($name, $status, $detail, $group) {
    $entry = @{
        name    = $name
        status  = $status   # PASS | FAIL | BLOCKED
        detail  = $detail
        group   = $group
        time    = (Get-Date -Format "o")
    }
    [void]$script:BrutalResults.Add($entry)

    if ($status -eq "PASS")     { $script:PassCount++; Write-Ts Green   "  PASS $name" }
    elseif ($status -eq "FAIL") { $script:FailCount++; Write-Ts Red     "  FAIL $name :: $detail" }
    else                        { $script:BlockedCount++; Write-Ts Yellow  "  BLOCKED $name :: $detail" }

    $currentGroup = if ($script:TestGroups.Count -gt 0) { $script:TestGroups.Peek() } else { "ungrouped" }
    $existing = $script:GroupStats | Where-Object { $_.group -eq $currentGroup }
    if ($existing) {
        if ($status -eq "PASS") { $existing.pass++ }
        elseif ($status -eq "FAIL") { $existing.fail++ }
        else { $existing.blocked++ }
    } else {
        [void]$script:GroupStats.Add(@{ group = $currentGroup; pass = $(if ($status -eq "PASS") { 1 } else { 0 }); fail = $(if ($status -eq "FAIL") { 1 } else { 0 }); blocked = $(if ($status -eq "BLOCKED") { 1 } else { 0 }) })
    }
}

# ─────────────────────────────────────────────────────────
# Authentication
# ─────────────────────────────────────────────────────────

function New-Login($email, $password) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    try {
        $csrf = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $session).data
    } catch {
        Write-Ts Yellow "  WARN: initial CSRF fetch failed: $($_.Exception.Message)"
        return @{ session = $null; headers = @{}; userId = $null; role = $null; branchId = $null; pharmacyId = $null }
    }

    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    try {
        $loginResp = Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
            -Body $loginBody -ContentType "application/json" `
            -WebSession $session -Headers @{ $csrf.headerName = $csrf.token }
    } catch {
        # Single-session constraint: the previous session may have been evicted.
        # Retry once with a fresh CSRF token.
        Start-Sleep -Milliseconds 300
        try {
            $csrf = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $session).data
            $loginResp = Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
                -Body $loginBody -ContentType "application/json" `
                -WebSession $session -Headers @{ $csrf.headerName = $csrf.token }
        } catch {
            Write-Ts Red "  Login failed for ${email}: $($_.Exception.Message)"
            return @{ session = $null; headers = @{}; userId = $null; role = $null; branchId = $null; pharmacyId = $null }
        }
    }

    # Post-login CSRF token (session cookie is now authenticated)
    $csrf2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $session).data

    # Extract user metadata from the login response or /auth/me
    $userId = $null; $role = $null; $branchId = $null; $pharmacyId = $null
    try {
        $meResp = Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/me" -WebSession $session -Headers @{ $csrf2.headerName = $csrf2.token }
        $user = $meResp.data.user
        $userId    = $user.id
        $pharmacyId = $user.pharmacyId
        $branchId   = $user.activeBranch.id
        $role       = $user.roles
    } catch {
        Write-Ts Yellow "  WARN: /auth/me failed after login: $($_.Exception.Message)"
    }

    return @{
        session    = $session
        headers    = @{ $csrf2.headerName = $csrf2.token; "Idempotency-Key" = [guid]::NewGuid().ToString() }
        userId     = $userId
        role       = $role
        branchId   = $branchId
        pharmacyId = $pharmacyId
    }
}

# ─────────────────────────────────────────────────────────
# API calls
# ─────────────────────────────────────────────────────────

function Call($method, $path, $ctx, $body) {
    if (-not $ctx -or -not $ctx.session) {
        return @{ ok = $false; data = $null; status = 0; error = "No session (ctx is null or session is null)"; raw = $null }
    }

    $headers = @{}
    foreach ($k in $ctx.headers.Keys) { $headers[$k] = $ctx.headers[$k] }

                    # Always generate a fresh idempotency key for mutating methods (unless already set)
                    if ($method -in @("POST", "PATCH", "PUT", "DELETE")) {
                        if (-not $headers.ContainsKey("Idempotency-Key") -or -not $headers["Idempotency-Key"]) {
                            $headers["Idempotency-Key"] = [guid]::NewGuid().ToString()
                        }
                    }

    $params = @{
        Uri        = "$ApiBase/api/v1$path"
        Method     = $method
        WebSession = $ctx.session
        Headers    = $headers
    }
    if ($body) {
        $params.ContentType = "application/json"
        if ($body -is [string]) { $params.Body = $body } else { $params.Body = ($body | ConvertTo-Json -Depth 20) }
    }

    try {
        $resp = Invoke-RestMethod @params
        return @{ ok = $true; data = $resp.data; status = 200; error = $null; raw = $resp }
    } catch {
        $statusCode = 0
        try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
        $msg = ""
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $msg = $sr.ReadToEnd()
        } catch { $msg = $_.Exception.Message }
        return @{ ok = $false; data = $null; status = $statusCode; error = $msg; raw = $_.Exception }
    }
}

function Call-Silent($method, $path, $ctx, $body) {
    return Call $method $path $ctx $body
}

# ─────────────────────────────────────────────────────────
# Assertions
# ─────────────────────────────────────────────────────────

function Assert($name, $condition, $detail) {
    $group = if ($script:TestGroups.Count -gt 0) { $script:TestGroups.Peek() } else { "ungrouped" }
    if ($condition) {
        Record $name "PASS" "" $group
    } else {
        Record $name "FAIL" ($detail -as [string]) $group
    }
    return [bool]$condition
}

function Assert-Ok($name, $result) {
    $group = if ($script:TestGroups.Count -gt 0) { $script:TestGroups.Peek() } else { "ungrouped" }
    if ($result.ok) {
        Record $name "PASS" "" $group
    } else {
        $detail = if ($result.error) { $result.error } else { "status=$($result.status)" }
        Record $name "FAIL" $detail $group
    }
    return [bool]$result.ok
}

function Assert-Fail($name, $result, $expectedErrorCode) {
    $group = if ($script:TestGroups.Count -gt 0) { $script:TestGroups.Peek() } else { "ungrouped" }
    if ($result.ok) {
        Record $name "FAIL" "Expected failure but got success" $group
        return $false
    }
    if ($expectedErrorCode) {
        $matches = $false
        if ($result.status -eq $expectedErrorCode) { $matches = $true }
        elseif ($result.error -match [regex]::Escape($expectedErrorCode.ToString())) { $matches = $true }
        if ($matches) {
            Record $name "PASS" "" $group
        } else {
            Record $name "FAIL" "Expected error $expectedErrorCode but got status=$($result.status) error=$($result.error)" $group
        }
        return $matches
    }
    Record $name "PASS" "" $group
    return $true
}

function Assert-Status($name, $result, $expectedStatus) {
    $group = if ($script:TestGroups.Count -gt 0) { $script:TestGroups.Peek() } else { "ungrouped" }
    if ($result.status -eq $expectedStatus) {
        Record $name "PASS" "" $group
    } else {
        Record $name "FAIL" "Expected HTTP $expectedStatus but got $($result.status)" $group
    }
    return ($result.status -eq $expectedStatus)
}

# ─────────────────────────────────────────────────────────
# Database helpers (docker exec → psql)
# ─────────────────────────────────────────────────────────

function Db-Query($sql) {
    $escaped = $sql -replace "'", "'\''"
    $raw = & docker exec $script:DbContainer psql -U $script:DbUser -d $script:DbName -t -A -F "`t" -c $sql 2>$null
    if (-not $raw) { return @() }

    $lines = $raw | Where-Object { $_.Trim() -ne "" }
    if (@($lines).Count -eq 0) { return @() }

    # First line contains column headers from the tab-separated output
    # With -t -A -F \t, psql does NOT print headers; we fall back to column count.
    # We return PSCustomObjects with generic property names.
    $objects = [System.Collections.ArrayList]::new()
    foreach ($line in $lines) {
        $fields = $line -split "`t"
        $obj = New-Object PSObject
        for ($i = 0; $i -lt $fields.Count; $i++) {
            $val = $fields[$i].Trim()
            # Try to cast numerics
            $num = 0.0
            if ([double]::TryParse($val, [ref]$num)) {
                $obj | Add-Member -NotePropertyName "col$i" -NotePropertyValue $num
            } else {
                $obj | Add-Member -NotePropertyName "col$i" -NotePropertyValue $val
            }
        }
        [void]$objects.Add($obj)
    }
    return @($objects.ToArray())
}

function Db-Scalar($sql) {
    $escaped = $sql -replace "'", "'\''"
    $raw = & docker exec $script:DbContainer psql -U $script:DbUser -d $script:DbName -t -A -c $sql 2>$null
    if ($null -eq $raw) { return $null }
    $val = ($raw | Out-String).Trim()
    if ($val -eq "") { return $null }

    $num = 0
    if ([int]::TryParse($val, [ref]$num)) { return $num }
    $dnum = 0.0
    if ([double]::TryParse($val, [ref]$dnum)) { return $dnum }
    return $val
}

# ─────────────────────────────────────────────────────────
# Reconciliation functions
# ─────────────────────────────────────────────────────────

function Reconcile-Sales($branchId) {
    $today = Get-Date -Format "yyyy-MM-dd"

    # Sum from the API
    $ctx = New-Login "admin@demo.com" "admin123"
    $apiResult = Call "GET" "/reports/sales-summary?branchId=$branchId&from=$today&to=$today" $ctx
    $apiTotal = 0.0
    $apiCount = 0
    if ($apiResult.ok -and $apiResult.data) {
        $apiTotal = [double]$apiResult.data.totalSales
        $apiCount = [int]$apiResult.data.salesCount
    }

    # Sum directly from DB
    $dbTotal = Db-Scalar "SELECT COALESCE(SUM(total), 0) FROM sales WHERE branch_id = '$branchId' AND DATE(completed_at) = '$today' AND status = 'COMPLETED'"
    $dbCount = Db-Scalar "SELECT COUNT(*) FROM sales WHERE branch_id = '$branchId' AND DATE(completed_at) = '$today' AND status = 'COMPLETED'"

    $totalDiff = [math]::Round([double]$apiTotal - [double]$dbTotal, 2)
    $countDiff = [int]$apiCount - [int]$dbCount

    return @{
        expected   = @{ total = [double]$dbTotal; count = [int]$dbCount }
        actual     = @{ total = [double]$apiTotal; count = [int]$apiCount }
        difference = @{ total = $totalDiff; count = $countDiff }
        ok         = ([math]::Abs($totalDiff) -lt 0.01 -and $countDiff -eq 0)
    }
}

function Reconcile-Inventory($branchId) {
    $ctx = New-Login "admin@demo.com" "admin123"
    $apiResult = Call "GET" "/stock?size=10000" $ctx
    $apiQty = 0
    $apiItemCount = 0
    if ($apiResult.ok -and $apiResult.data) {
        foreach ($row in @($apiResult.data.content)) {
            $apiQty += [int]$row.quantityAvailable
            $apiItemCount++
        }
    }

    $dbQty = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE branch_id = '$branchId' AND quantity_available > 0"
    $dbItemCount = Db-Scalar "SELECT COUNT(*) FROM medicine_batches WHERE branch_id = '$branchId' AND quantity_available > 0"

    $qtyDiff = [int]$apiQty - [int]$dbQty
    $itemDiff = [int]$apiItemCount - [int]$dbItemCount

    return @{
        expected   = @{ quantity = [int]$dbQty; items = [int]$dbItemCount }
        actual     = @{ quantity = [int]$apiQty; items = [int]$apiItemCount }
        difference = @{ quantity = $qtyDiff; items = $itemDiff }
        ok         = ($qtyDiff -eq 0 -and $itemDiff -eq 0)
    }
}

function Reconcile-CashDrawer($shiftId) {
    $ctx = New-Login "admin@demo.com" "admin123"
    $shiftResult = Call "GET" "/shifts/$shiftId" $ctx
    $apiExpected = 0.0
    $apiActual = 0.0
    $apiVariance = 0.0
    if ($shiftResult.ok -and $shiftResult.data) {
        $apiExpected = [double]$shiftResult.data.expectedCash
        $apiActual   = [double]$shiftResult.data.actualCash
        $apiVariance = [double]$shiftResult.data.variance
    }

    # Recalculate from DB: opening + cash_in - cash_out + cash_sales - cash_refunds
    $openingFloat = Db-Scalar "SELECT COALESCE(opening_float, 0) FROM staff_shifts WHERE id = '$shiftId'"
    $cashIn = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE shift_id = '$shiftId' AND transaction_type = 'CASH_IN'"
    $cashOut = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE shift_id = '$shiftId' AND transaction_type = 'CASH_OUT'"
    $cashSales = Db-Scalar "SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN sales s ON p.sale_id = s.id WHERE s.shift_id = '$shiftId' AND p.method = 'CASH' AND s.status = 'COMPLETED'"

    $dbExpected = [double]$openingFloat + [double]$cashIn - [double]$cashOut + [double]$cashSales
    $diff = [math]::Round($apiExpected - $dbExpected, 2)

    return @{
        expected   = @{ cash = [math]::Round($dbExpected, 2) }
        actual     = @{ cash = $apiExpected; variance = $apiVariance }
        difference = @{ cash = $diff }
        ok         = ([math]::Abs($diff) -lt 0.01)
    }
}

function Reconcile-Audit($fromDate, $toDate) {
    $ctx = New-Login "admin@demo.com" "admin123"
    $auditResult = Call "GET" "/audit-logs?size=10000&fromDate=$fromDate&toDate=$toDate" $ctx
    $apiCount = 0
    $apiActions = @()
    if ($auditResult.ok -and $auditResult.data) {
        $apiCount = @($auditResult.data.content).Count
        $apiActions = @($auditResult.data.content | ForEach-Object { $_.action })
    }

    $dbCount = Db-Scalar "SELECT COUNT(*) FROM audit_logs WHERE DATE(created_at) BETWEEN '$fromDate' AND '$toDate'"

    $diff = [int]$apiCount - [int]$dbCount

    # Check required action coverage
    $requiredActions = @(
        "OPEN_SHIFT", "CLOSE_SHIFT", "CREATE_SALE", "CREATE_PURCHASE_ORDER",
        "APPROVE_PURCHASE_ORDER", "CREATE_USER", "LOGIN"
    )
    $missing = $requiredActions | Where-Object { $apiActions -notcontains $_ }

    return @{
        expected   = @{ count = [int]$dbCount; requiredActions = $requiredActions }
        actual     = @{ count = [int]$apiCount; actions = ($apiActions | Select-Object -Unique) }
        difference = @{ count = $diff; missingActions = $missing }
        ok         = ([math]::Abs($diff) -le 5 -and $missing.Count -eq 0)   # allow small timing slack
    }
}

# ─────────────────────────────────────────────────────────
# Concurrency
# ─────────────────────────────────────────────────────────

function Invoke-Parallel($scriptBlocks) {
    $runspacePool = [runspacefactory]::CreateRunspacePool(1, [Math]::Min($scriptBlocks.Count, 10))
    $runspacePool.Open()

    $jobs = [System.Collections.ArrayList]::new()
    foreach ($sb in $scriptBlocks) {
        $ps = [powershell]::Create()
        $ps.RunspacePool = $runspacePool
        [void]$ps.AddScript($sb)
        $handle = $ps.BeginInvoke()
        [void]$jobs.Add(@{ powershell = $ps; handle = $handle })
    }

    $results = [System.Collections.ArrayList]::new()
    foreach ($job in $jobs) {
        $job.handle.AsyncWaitHandle.WaitOne() | Out-Null
        try {
            $output = $job.powershell.EndInvoke($job.handle)
            [void]$results.Add(@{ ok = $true; data = $output; error = $null })
        } catch {
            [void]$results.Add(@{ ok = $false; data = $null; error = $_.Exception.Message })
        } finally {
            $job.powershell.Dispose()
        }
    }

    $runspacePool.Close()
    $runspacePool.Dispose()
    return $results.ToArray()
}

# ─────────────────────────────────────────────────────────
# Chaos engineering — Docker container control
# ─────────────────────────────────────────────────────────

function Stop-ApiContainer() {
    Write-Ts Yellow "  CHAOS: Stopping API container ($script:ContainerName)..."
    & docker stop $script:ContainerName 2>$null | Out-Null
    Start-Sleep -Seconds 1
}

function Start-ApiContainer() {
    Write-Ts Yellow "  CHAOS: Starting API container ($script:ContainerName)..."
    & docker start $script:ContainerName 2>$null | Out-Null
}

function Restart-ApiContainer() {
    Write-Ts Yellow "  CHAOS: Restarting API container ($script:ContainerName)..."
    & docker restart $script:ContainerName 2>$null | Out-Null
}

function Restart-DockerStack() {
    Write-Ts Yellow "  CHAOS: Restarting entire Docker stack..."
    Push-Location $script:DockerComposeDir
    & docker compose restart 2>$null | Out-Null
    Pop-Location
}

function Stop-Database() {
    Write-Ts Yellow "  CHAOS: Stopping database container ($script:DbContainer)..."
    & docker stop $script:DbContainer 2>$null | Out-Null
    Start-Sleep -Seconds 1
}

function Start-Database() {
    Write-Ts Yellow "  CHAOS: Starting database container ($script:DbContainer)..."
    & docker start $script:DbContainer 2>$null | Out-Null
}

# ─────────────────────────────────────────────────────────
# Test group tracking
# ─────────────────────────────────────────────────────────

function Begin-TestGroup($name) {
    $script:TestGroups.Push($name)
    Write-Host ""
    Write-Ts Cyan "== $name =="
}

function End-TestGroup() {
    if ($script:TestGroups.Count -gt 0) {
        $script:TestGroups.Pop() | Out-Null
    }
}

function Get-TestReport() {
    $report = @{
        timestamp   = (Get-Date -Format "o")
        totalPass   = $script:PassCount
        totalFail   = $script:FailCount
        totalBlocked = $script:BlockedCount
        total       = $script:PassCount + $script:FailCount + $script:BlockedCount
        passRate    = if (($script:PassCount + $script:FailCount + $script:BlockedCount) -gt 0) {
            [math]::Round(($script:PassCount / ($script:PassCount + $script:FailCount + $script:BlockedCount)) * 100, 1)
        } else { 0 }
        groups      = $script:GroupStats.ToArray()
        results     = $script:BrutalResults.ToArray()
    }
    return ($report | ConvertTo-Json -Depth 10)
}

# ─────────────────────────────────────────────────────────
# Wait / polling helpers
# ─────────────────────────────────────────────────────────

function Wait-ForApi($timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    $attempt = 0
    while ((Get-Date) -lt $deadline) {
        $attempt++
        try {
            $resp = Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -TimeoutSec 3
            if ($resp) { Write-Ts Green "  API healthy after $attempt attempt(s)"; return $true }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    Write-Ts Red "  API did not become healthy within ${timeoutSeconds}s"
    return $false
}

function Wait-ForHealthy($service, $timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    $attempt = 0
    while ((Get-Date) -lt $deadline) {
        $attempt++
        $status = & docker inspect --format='{{.State.Health.Status}}' $service 2>$null
        if ($LASTEXITCODE -ne 0) {
            # Container may not have healthcheck; check if it's at least running
            $running = & docker inspect --format='{{.State.Running}}' $service 2>$null
            if ($running -eq "true") { Write-Ts Green "  $service is running (no healthcheck)"; return $true }
        }
        if ($status -match "healthy") { Write-Ts Green "  $service healthy after $attempt attempt(s)"; return $true }
        Start-Sleep -Seconds 2
    }
    Write-Ts Red "  $service did not become healthy within ${timeoutSeconds}s"
    return $false
}

# ─────────────────────────────────────────────────────────
# Get-CurrentShift — find the active shift for a user context
# ─────────────────────────────────────────────────────────

function Get-CurrentShift($ctx) {
    $me = Call "GET" "/auth/me" $ctx
    if (-not $me.ok) { return $null }
    $uid = $me.data.user.id
    $shifts = Call "GET" "/shifts?userId=$uid&size=20" $ctx
    if (-not $shifts.ok) { return $null }
    $active = @($shifts.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
    return $active
}

# ─────────────────────────────────────────────────────────
# Print summary (call at end of test script)
# ─────────────────────────────────────────────────────────

function Write-Summary {
    $total = $script:PassCount + $script:FailCount + $script:BlockedCount
    Write-Host ""
    Write-Host "========================================" -ForegroundColor White
    Write-Host "  BRUTAL TEST SUMMARY" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor White
    Write-Host ("  PASSED:   {0}" -f $script:PassCount) -ForegroundColor Green
    Write-Host ("  FAILED:   {0}" -f $script:FailCount) -ForegroundColor Red
    Write-Host ("  BLOCKED:  {0}" -f $script:BlockedCount) -ForegroundColor Yellow
    Write-Host ("  TOTAL:    {0}" -f $total) -ForegroundColor White
    if ($total -gt 0) {
        $rate = [math]::Round(($script:PassCount / $total) * 100, 1)
        Write-Host ("  PASS RATE: {0}%" -f $rate) -ForegroundColor White
    }
    Write-Host "========================================" -ForegroundColor White

    if ($script:BrutalResults.Count -gt 0) {
        Write-Host ""
        Write-Host "  Failures:" -ForegroundColor Red
        $script:BrutalResults | Where-Object { $_.status -eq "FAIL" } | ForEach-Object {
            Write-Host ("    - [{0}] {1}" -f $_.group, $_.name) -ForegroundColor Red
            if ($_.detail) { Write-Host ("      {0}" -f $_.detail) -ForegroundColor DarkRed }
        }
    }
}

Write-Host "[brutal-common.ps1] Loaded - ApiBase=$ApiBase, DbContainer=$($script:DbContainer)" -ForegroundColor DarkGray
