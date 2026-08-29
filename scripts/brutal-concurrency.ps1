. "$PSScriptRoot\brutal-common.ps1"

# ═══════════════════════════════════════════════════════════
# C1: Last-Unit Oversell
# ═══════════════════════════════════════════════════════════
Begin-TestGroup "C1: Last-Unit Oversell"

try {
    $adminCtx = New-Login "admin@demo.com" "admin123"
    $branchId = $adminCtx.branchId

    $medsResp = Call "GET" "/medicines?size=200" $adminCtx
    $paracetamol = @($medsResp.data.content | Where-Object { $_.brandName -like "*Paracetamol*" }) | Select-Object -First 1
    $medId = $paracetamol.id
    $unitPrice = if ($paracetamol.sellingPrice) { [double]$paracetamol.sellingPrice } else { 40.00 }

    $stockRows = Db-Query "SELECT COALESCE(SUM(s.quantity_available), 0) FROM stock s JOIN medicine_batches mb ON s.medicine_batches_id = mb.id WHERE mb.medicine_id = '$medId' AND s.branch_id = '$branchId'"
    $currentStock = [int]($stockRows[0].col0)
    Write-Ts Yellow "  Current stock: $currentStock"

    if ($currentStock -lt 2) {
        Assert "C1-stock-available" $false "Need at least 2 units, have $currentStock"
        throw "SKIP"
    }

    # Set stock to exactly 1
    $batchRows = Db-Query "SELECT mb.id FROM medicine_batches mb JOIN stock s ON s.medicine_batches_id = mb.id WHERE mb.medicine_id = '$medId' AND s.branch_id = '$branchId' AND s.quantity_available > 0 ORDER BY mb.expiration_date ASC LIMIT 1"
    $batchId = $batchRows[0].col0
    Db-Query "UPDATE stock SET quantity_available = 1 WHERE medicine_batches_id = '$batchId' AND branch_id = '$branchId'"
    Write-Ts Yellow "  Set stock to 1"

    # Get shifts
    $cashier1Ctx = New-Login "cashier@demo.com" "cashier123"
    $cashier2Ctx = New-Login "storekeeper@demo.com" "stock1234"

    $shift1Resp = Call "GET" "/shifts/active/user/$($cashier1Ctx.userId)" $cashier1Ctx
    $shift1 = $shift1Resp.data
    if (-not $shift1) {
        $shift1Resp = Call "POST" "/shifts" $cashier1Ctx @{ shiftName = "C1-1"; openingFloat = 10000 }
        $shift1 = $shift1Resp.data
    }
    $shift2Resp = Call "GET" "/shifts/active/user/$($cashier2Ctx.userId)" $cashier2Ctx
    $shift2 = $shift2Resp.data
    if (-not $shift2) {
        $shift2Resp = Call "POST" "/shifts" $cashier2Ctx @{ shiftName = "C1-2"; openingFloat = 10000 }
        $shift2 = $shift2Resp.data
    }

    # Build sale bodies - Idempotency-Key must match clientSaleId
    $key1 = [guid]::NewGuid().ToString()
    $key2 = [guid]::NewGuid().ToString()
    $saleBody1 = @{
        clientSaleId = $key1; shiftId = $shift1.id
        items = @(@{ medicineId = $medId; quantity = 1; unitPrice = $unitPrice; expectedUnitPrice = $unitPrice; lineId = [guid]::NewGuid().ToString() })
        payments = @(@{ method = "CASH"; amount = $unitPrice }); cashTendered = $unitPrice
    }
    $saleBody2 = @{
        clientSaleId = $key2; shiftId = $shift2.id
        items = @(@{ medicineId = $medId; quantity = 1; unitPrice = $unitPrice; expectedUnitPrice = $unitPrice; lineId = [guid]::NewGuid().ToString() })
        payments = @(@{ method = "CASH"; amount = $unitPrice }); cashTendered = $unitPrice
    }

    # Get session cookies for passing to jobs
    $cookie1 = $cashier1Ctx.session.Cookies.GetCookies("http://localhost:9090")["pos_session"].Value
    $cookie2 = $cashier2Ctx.session.Cookies.GetCookies("http://localhost:9090")["pos_session"].Value

    # Override Idempotency-Key to match clientSaleId
    $headers1 = @{}
    foreach ($k in $cashier1Ctx.headers.Keys) { $headers1[$k] = $cashier1Ctx.headers[$k] }
    $headers1["Idempotency-Key"] = $key1

    $headers2 = @{}
    foreach ($k in $cashier2Ctx.headers.Keys) { $headers2[$k] = $cashier2Ctx.headers[$k] }
    $headers2["Idempotency-Key"] = $key2

    # Fire concurrently using Start-Job
    $job1 = Start-Job -ScriptBlock {
        param($apiBase, $body, $headers, $sessionCookie)
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $cookie = New-Object System.Net.Cookie("pos_session", $sessionCookie, "/", "localhost")
        $session.Cookies.Add($cookie)
        try {
            $resp = Invoke-RestMethod -Uri "$apiBase/api/v1/sales" -Method Post -Body ($body | ConvertTo-Json -Depth 20) -ContentType "application/json" -WebSession $session -Headers $headers
            return @{ ok = $true; data = $resp.data }
        } catch {
            $statusCode = 0
            try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
            $msg = ""
            try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { $msg = $_.Exception.Message }
            return @{ ok = $false; status = $statusCode; error = $msg }
        }
    } -ArgumentList $ApiBase, $saleBody1, $headers1, $cookie1

    $job2 = Start-Job -ScriptBlock {
        param($apiBase, $body, $headers, $sessionCookie)
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $cookie = New-Object System.Net.Cookie("pos_session", $sessionCookie, "/", "localhost")
        $session.Cookies.Add($cookie)
        try {
            $resp = Invoke-RestMethod -Uri "$apiBase/api/v1/sales" -Method Post -Body ($body | ConvertTo-Json -Depth 20) -ContentType "application/json" -WebSession $session -Headers $headers
            return @{ ok = $true; data = $resp.data }
        } catch {
            $statusCode = 0
            try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
            $msg = ""
            try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { $msg = $_.Exception.Message }
            return @{ ok = $false; status = $statusCode; error = $msg }
        }
    } -ArgumentList $ApiBase, $saleBody2, $headers2, $cookie2

    $r1 = Receive-Job $job1 -Wait
    $r2 = Receive-Job $job2 -Wait
    Remove-Job $job1, $job2

    $okCount = @($r1, $r2 | Where-Object { $_.ok }).Count
    $failCount = @($r1, $r2 | Where-Object { -not $_.ok }).Count

    Assert "C1-exactly-one-succeeds" ($okCount -eq 1) "Expected 1 success, got $okCount"
    Assert "C1-exactly-one-fails" ($failCount -eq 1) "Expected 1 failure, got $failCount"

    # Verify stock for the specific batch we set to 1
    $stockAfter = Db-Query "SELECT COALESCE(SUM(s.quantity_available), 0) FROM stock s WHERE s.medicine_batches_id = '$batchId' AND s.branch_id = '$branchId'"
    $finalStock = [int]($stockAfter[0].col0)
    Assert "C1-batch-stock-is-zero" ($finalStock -eq 0) "Expected batch stock 0, got $finalStock"
    Assert "C1-batch-stock-not-negative" ($finalStock -ge 0) "Batch stock went negative: $finalStock"

    # Restore stock
    Db-Query "UPDATE stock SET quantity_available = 500 WHERE medicine_batches_id = '$batchId' AND branch_id = '$branchId'"

} catch {
    if ($_.Exception.Message -ne "SKIP") {
        Assert "C1-unhandled-error" $false $_.Exception.Message
    }
}

End-TestGroup

# ═══════════════════════════════════════════════════════════
# C2: Double-Click Idempotency
# ═══════════════════════════════════════════════════════════
Begin-TestGroup "C2: Double-Click Idempotency"

try {
    $ctx = New-Login "cashier@demo.com" "cashier123"
    $shiftResp = Call "GET" "/shifts/active/user/$($ctx.userId)" $ctx
    $shift = $shiftResp.data
    if (-not $shift) {
        $shiftResp = Call "POST" "/shifts" $ctx @{ shiftName = "C2 Shift"; openingFloat = 10000 }
        $shift = $shiftResp.data
    }

    $medsResp = Call "GET" "/medicines?size=200" $ctx
    $med = @($medsResp.data.content | Where-Object { $_.brandName -like "*Paracetamol*" }) | Select-Object -First 1
    $price = if ($med.sellingPrice) { [double]$med.sellingPrice } else { 40.00 }

    $idempotencyKey = [guid]::NewGuid().ToString()
    $saleBody = @{
        clientSaleId = $idempotencyKey; shiftId = $shift.id
        items = @(@{ medicineId = $med.id; quantity = 1; unitPrice = $price; expectedUnitPrice = $price; lineId = [guid]::NewGuid().ToString() })
        payments = @(@{ method = "CASH"; amount = $price }); cashTendered = $price
    }

    # Override Idempotency-Key to match clientSaleId
    $saleHeaders = @{}
    foreach ($k in $ctx.headers.Keys) { $saleHeaders[$k] = $ctx.headers[$k] }
    $saleHeaders["Idempotency-Key"] = $idempotencyKey

    # First request
    $r1 = Call "POST" "/sales" $ctx $saleBody
    # Manually set the Idempotency-Key for this call
    $ctx.headers["Idempotency-Key"] = $idempotencyKey
    $r1 = Call "POST" "/sales" $ctx $saleBody
    Assert "C2-first-sale-succeeds" $r1.ok "First sale should succeed: $($r1.error)"

    # Second request with SAME idempotency key
    $r2 = Call "POST" "/sales" $ctx $saleBody
    Assert "C2-second-sale-returns-cached" $r2.ok "Second request should return cached: $($r2.error)"

    # Verify only one sale
    $count = Db-Query "SELECT COUNT(*) FROM sales WHERE client_sale_id = '$idempotencyKey'"
    $saleCount = [int]($count[0].col0)
    Assert "C2-only-one-sale" ($saleCount -eq 1) "Expected 1 sale, got $saleCount"

} catch {
    Assert "C2-unhandled-error" $false $_.Exception.Message
}

End-TestGroup

# ═══════════════════════════════════════════════════════════
# C3: Concurrent Shift Open
# ═══════════════════════════════════════════════════════════
Begin-TestGroup "C3: Concurrent Shift Open"

try {
    $ctx = New-Login "cashier@demo.com" "cashier123"

    # Close any existing shift
    $activeShifts = Db-Query "SELECT id FROM staff_shifts WHERE user_id = '$($ctx.userId)' AND status = 'ACTIVE'"
    foreach ($s in $activeShifts) {
        Db-Query "UPDATE staff_shifts SET status = 'CLOSED', shift_end_time = NOW() WHERE id = '$($s.col0)'"
        Db-Query "UPDATE cash_drawers SET status = 'CLOSED', closing_time = NOW()::time WHERE staff_shifts_id = '$($s.col0)' AND status = 'OPEN'"
    }

    # Try to open two shifts simultaneously
    $cookie = $ctx.session.Cookies.GetCookies("http://localhost:9090")["pos_session"].Value
    $body1 = @{ shiftName = "C3-A"; openingFloat = 5000 } | ConvertTo-Json
    $body2 = @{ shiftName = "C3-B"; openingFloat = 5000 } | ConvertTo-Json

    $job1 = Start-Job -ScriptBlock {
        param($apiBase, $body, $headers, $sessionCookie)
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $cookie = New-Object System.Net.Cookie("pos_session", $sessionCookie, "/", "localhost")
        $session.Cookies.Add($cookie)
        try {
            $resp = Invoke-RestMethod -Uri "$apiBase/api/v1/shifts" -Method Post -Body $body -ContentType "application/json" -WebSession $session -Headers $headers
            return @{ ok = $true; data = $resp.data }
        } catch {
            $statusCode = 0; try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
            $msg = ""; try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { $msg = $_.Exception.Message }
            return @{ ok = $false; status = $statusCode; error = $msg }
        }
    } -ArgumentList $ApiBase, $body1, $ctx.headers, $cookie

    $job2 = Start-Job -ScriptBlock {
        param($apiBase, $body, $headers, $sessionCookie)
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $cookie = New-Object System.Net.Cookie("pos_session", $sessionCookie, "/", "localhost")
        $session.Cookies.Add($cookie)
        try {
            $resp = Invoke-RestMethod -Uri "$apiBase/api/v1/shifts" -Method Post -Body $body -ContentType "application/json" -WebSession $session -Headers $headers
            return @{ ok = $true; data = $resp.data }
        } catch {
            $statusCode = 0; try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
            $msg = ""; try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { $msg = $_.Exception.Message }
            return @{ ok = $false; status = $statusCode; error = $msg }
        }
    } -ArgumentList $ApiBase, $body2, $ctx.headers, $cookie

    $r1 = Receive-Job $job1 -Wait
    $r2 = Receive-Job $job2 -Wait
    Remove-Job $job1, $job2

    $okCount = @($r1, $r2 | Where-Object { $_.ok }).Count
    $failCount = @($r1, $r2 | Where-Object { -not $_.ok }).Count

    Assert "C3-exactly-one-shift-opens" ($okCount -eq 1) "Expected 1 success, got $okCount"
    Assert "C3-other-rejected" ($failCount -eq 1) "Expected 1 rejection, got $failCount"

    # Verify only one active shift
    $active = Db-Query "SELECT COUNT(*) FROM staff_shifts WHERE user_id = '$($ctx.userId)' AND status = 'ACTIVE'"
    Assert "C3-one-active-shift" ([int]($active[0].col0) -eq 1) "Expected 1 active shift"

} catch {
    Assert "C3-unhandled-error" $false $_.Exception.Message
}

End-TestGroup

Write-Summary
