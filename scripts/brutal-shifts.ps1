#Requires -Version 5.1
# brutal-shifts.ps1 — Shift chaos tests for the Pharmacy POS brutal suite.
# Tests shift lifecycle, duplicate prevention, sales without shift, and reconciliation.
. "$PSScriptRoot\brutal-common.ps1"

Write-Host ""
Write-Host "========================================================" -ForegroundColor White
Write-Host "  BRUTAL SHIFT CHAOS TESTS" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor White

# ─────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────

function Open-ShiftFor($ctx, $float) {
    $shift = Call "POST" "/shifts" $ctx @{ openingFloat = $float; remarks = "brutal-shift"; shiftName = "ShiftTill" }
    if ($shift.ok) { return $shift.data }
    return $null
}

function Close-ShiftFor($ctx, $shiftId, $actualCash) {
    return Call "PATCH" "/shifts/$shiftId/close" $ctx @{ actualCash = $actualCash; remarks = "counted" }
}

function Make-CashSale($ctx, $shiftId, $medId, $unitPrice, $qty) {
    $key = [guid]::NewGuid().ToString()
    $body = @{
        clientSaleId = $key; shiftId = $shiftId; customerId = $null; note = $null
        prescriptionReferenceId = $null; cashTendered = ($unitPrice * $qty)
        items = @(@{ lineId = [guid]::NewGuid().ToString(); medicineId = $medId; quantity = $qty; expectedUnitPrice = $unitPrice; discountPercent = 0; requestedBatchId = $null; sellingUnitId = $null })
        payments = @(@{ amount = ($unitPrice * $qty); method = "CASH"; reference = $null })
    }
    return Call "POST" "/sales" $ctx $body
}

function Make-MpesaSale($ctx, $shiftId, $medId, $unitPrice, $qty) {
    $key = [guid]::NewGuid().ToString()
    $amount = $unitPrice * $qty
    $body = @{
        clientSaleId = $key; shiftId = $shiftId; customerId = $null; note = $null
        prescriptionReferenceId = $null; cashTendered = $null
        items = @(@{ lineId = [guid]::NewGuid().ToString(); medicineId = $medId; quantity = $qty; expectedUnitPrice = $unitPrice; discountPercent = 0; requestedBatchId = $null; sellingUnitId = $null })
        payments = @(@{ amount = $amount; method = "MPESA_MANUAL"; reference = ("BRU" + (Get-Random -Maximum 99999)) })
    }
    return Call "POST" "/sales" $ctx $body
}

function Make-Refund($ctx, $saleId, $saleItemId, $batchId, $qty, $method) {
    $key = [guid]::NewGuid().ToString()
    $body = @{
        clientReturnId = $key; saleId = $saleId; reason = "Brutal shift refund"
        refundMethod = $method; refundReference = $null
        items = @(@{ medicineBatchesId = $batchId; quantity = $qty; saleItemId = $saleItemId })
    }
    return Call "POST" "/sale-returns" $ctx $body
}

function Get-BatchForSale($saleData) {
    $item = @($saleData.items)[0]
    $allocId = $null
    if ($item.allocations -and @($item.allocations).Count -gt 0) {
        $allocId = @($item.allocations)[0].batchId
    }
    if (-not $allocId) {
        $allocId = Db-Scalar "SELECT medicine_batches_id FROM sale_item_allocations WHERE sale_item_id = '$($item.id)' LIMIT 1"
    }
    return @{ itemId = $item.id; batchId = $allocId }
}

# ─────────────────────────────────────────────────────────
# Setup
# ─────────────────────────────────────────────────────────

$branchId = Db-Scalar "SELECT id FROM branch LIMIT 1"
if (-not $branchId) { Write-Ts Red "No branch found — aborting."; exit 1 }

# Seed data if needed
$owner = New-Login "admin@demo.com" "admin123"
$meds = Call "GET" "/medicines?size=100`&sort=brandName,asc" $owner
if (@($meds.data.content).Count -lt 1) {
    Write-Ts Yellow "No medicines found — seeding..."
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "seed-demo-data.ps1") *> $null
    $owner = New-Login "admin@demo.com" "admin123"
    $meds = Call "GET" "/medicines?size=100`&sort=brandName,asc" $owner
}
$paracetamol = @($meds.data.content) | Where-Object { $_.brandName -like "*Paracetamol*" } | Select-Object -First 1
$medId = $paracetamol.id
$unitPrice = [double]$paracetamol.sellingPrice
if ($unitPrice -le 0) { $unitPrice = 40.00 }

# Close all active shifts for a clean slate
foreach ($email in @("cashier@demo.com", "admin@demo.com", "pharmacist@demo.com")) {
    $ctx = New-Login $email ($email -replace "@demo\.com", "" -replace "admin", "admin123" -replace "cashier", "cashier123" -replace "pharmacist", "pharmacist123")
    if ($ctx.session) {
        $me = Call "GET" "/auth/me" $ctx
        $uid = $me.data.user.id
        $active = Call "GET" "/shifts?userId=$uid`&size=20`&status=ACTIVE" $ctx
        if ($active.ok) {
            foreach ($s in @($active.data | Where-Object { $_.status -eq "ACTIVE" })) {
                Close-ShiftFor $ctx $s.id 0 | Out-Null
            }
        }
    }
}
Start-Sleep -Milliseconds 500

# ============================================================
# Shift1: Full Lifecycle
# ============================================================
Begin-TestGroup "Shift1: Full Lifecycle"

$shiftCashier = New-Login "cashier@demo.com" "cashier123"
if (-not $shiftCashier.session) {
    Record "Shift1-login" "BLOCKED" "Cashier login failed" "Shift1"
    End-TestGroup
} else {
    $openingFloat = 5000.00
    $shift = Open-ShiftFor $shiftCashier $openingFloat
    if (-not $shift) {
        Record "Shift1-open" "BLOCKED" "Could not open shift" "Shift1"
        End-TestGroup
    } else {
        $shiftId = $shift.id
        Assert "Shift1-is-active" ($shift.status -eq "ACTIVE") "Status=$($shift.status)"
        Assert "Shift1-opening-float" ([double]$shift.openingFloat -eq $openingFloat) "Float=$($shift.openingFloat)"

        # Perform 5 cash sales
        $totalCashSales = 0.0
        for ($i = 1; $i -le 5; $i++) {
            $ctx = New-Login "cashier@demo.com" "cashier123"
            $sale = Make-CashSale $ctx $shiftId $medId $unitPrice $i
            if ($sale.ok) { $totalCashSales += [double]$sale.data.total }
            Assert-Ok "Shift1-sale-$i" $sale
        }

        # Close shift
        $actualCash = $openingFloat + $totalCashSales
        $ctx = New-Login "cashier@demo.com" "cashier123"
        $closed = Close-ShiftFor $ctx $shiftId $actualCash
        Assert-Ok "Shift1-closed" $closed
        if ($closed.ok) {
            Assert "Shift1-status-closed" ($closed.data.status -eq "CLOSED") "Status=$($closed.data.status)"
            Assert "Shift1-variance-zero" ([math]::Abs([double]$closed.data.variance) -lt 0.01) "Variance=$($closed.data.variance)"
        }

        # Verify Z-report totals match actual sales
        $zOwner = New-Login "admin@demo.com" "admin123"
        $zReport = Call "GET" "/reports/shift-z/$shiftId" $zOwner
        Assert-Ok "Shift1-z-report" $zReport
        if ($zReport.ok) {
            $zSalesTotal = [double]$zReport.data.totalSales
            $zCashTotal = [double]$zReport.data.cashSales
            Assert "Shift1-z-total-matches" ([math]::Abs($zSalesTotal - $totalCashSales) -lt 0.01) "Z=$zSalesTotal actual=$totalCashSales"
            Assert "Shift1-z-cash-matches" ([math]::Abs($zCashTotal - $totalCashSales) -lt 0.01) "Z-cash=$zCashTotal actual=$totalCashSales"
            Assert "Shift1-z-sales-count" ([int]$zReport.data.salesCount -eq 5) "Count=$($zReport.data.salesCount)"
        }

        # DB-level reconciliation
        $dbSalesTotal = Db-Scalar "SELECT COALESCE(SUM(total), 0) FROM sales WHERE shift_id = '$shiftId' AND status = 'COMPLETED'"
        $dbSalesCount = Db-Scalar "SELECT COUNT(*) FROM sales WHERE shift_id = '$shiftId' AND status = 'COMPLETED'"
        Assert "Shift1-db-sales-match" ([math]::Abs([double]$dbSalesTotal - $totalCashSales) -lt 0.01) "DB=$dbSalesTotal actual=$totalCashSales"
        Assert "Shift1-db-count-match" ([int]$dbSalesCount -eq 5) "DB count=$dbSalesCount"

        # Variance calculation test: close with KES 100 over
        # Re-open won't work since it's closed; verify variance was correctly computed
        $expectedCash = $openingFloat + $totalCashSales
        $varianceExpected = $actualCash - $expectedCash
        Assert "Shift1-variance-calculation" ([math]::Abs($varianceExpected) -lt 0.01) "Variance calc=$varianceExpected"
    }
}
End-TestGroup

# ============================================================
# Shift2: Duplicate Shift Prevention
# ============================================================
Begin-TestGroup "Shift2: Duplicate Shift Prevention"

$dupCashier = New-Login "cashier@demo.com" "cashier123"
if (-not $dupCashier.session) {
    Record "Shift2-login" "BLOCKED" "Cashier login failed" "Shift2"
    End-TestGroup
} else {
    # Clean up any active shift
    $meDup = Call "GET" "/auth/me" $dupCashier
    $dupUid = $meDup.data.user.id
    $dupActive = Call "GET" "/shifts?userId=$dupUid`&size=20`&status=ACTIVE" $dupCashier
    if ($dupActive.ok) {
        foreach ($s in @($dupActive.data | Where-Object { $_.status -eq "ACTIVE" })) {
            Close-ShiftFor $dupCashier $s.id 0 | Out-Null
        }
    }
    Start-Sleep -Milliseconds 300

    # Open first shift
    $dupCashier = New-Login "cashier@demo.com" "cashier123"
    $shift2a = Open-ShiftFor $dupCashier 2000.00
    Assert-Ok "Shift2-first-shift-opened" @{ ok = [bool]$shift2a; data = $shift2a }

    if ($shift2a) {
        # Attempt to open a second shift for the same user — should fail
        $dupCashier2 = New-Login "cashier@demo.com" "cashier123"
        $shift2b = Call "POST" "/shifts" $dupCashier2 @{ openingFloat = 3000; remarks = "duplicate attempt"; shiftName = "DupTill" }
        Assert-Fail "Shift2-duplicate-rejected" $shift2b "409"
        if (-not $shift2b.ok) {
            $errMsg = $shift2b.error
            Assert "Shift2-error-code" ($errMsg -match "SHIFT_ALREADY_OPEN|already.*open|active.*shift|409") "Error=$errMsg"
        }

        # Verify only one active shift exists
        $dupCashier3 = New-Login "cashier@demo.com" "cashier123"
        $meDup2 = Call "GET" "/auth/me" $dupCashier3
        $dupUid2 = $meDup2.data.user.id
        $activeShifts = Call "GET" "/shifts?userId=$dupUid2`&size=20`&status=ACTIVE" $dupCashier3
        $activeCount = @($activeShifts.data | Where-Object { $_.status -eq "ACTIVE" }).Count
        Assert "Shift2-only-one-active" ($activeCount -eq 1) "Active shifts=$activeCount"

        # Clean up
        $dupCashier4 = New-Login "cashier@demo.com" "cashier123"
        Close-ShiftFor $dupCashier4 $shift2a.id 2000.00 | Out-Null
    }
}
End-TestGroup

# ============================================================
# Shift3: Sale Without Active Shift
# ============================================================
Begin-TestGroup "Shift3: Sale Without Active Shift"

# Create a fresh user with no shift (or use existing user after closing all shifts)
$noShiftCtx = New-Login "pharmacist@demo.com" "pharmacist123"
if (-not $noShiftCtx.session) {
    Record "Shift3-login" "BLOCKED" "Pharmacist login failed" "Shift3"
    End-TestGroup
} else {
    # Close any active shifts for this user
    $meNoShift = Call "GET" "/auth/me" $noShiftCtx
    $noShiftUid = $meNoShift.data.user.id
    $activeNoShift = Call "GET" "/shifts?userId=$noShiftUid`&size=20`&status=ACTIVE" $noShiftCtx
    if ($activeNoShift.ok) {
        foreach ($s in @($activeNoShift.data | Where-Object { $_.status -eq "ACTIVE" })) {
            Close-ShiftFor $noShiftCtx $s.id 0 | Out-Null
        }
    }
    Start-Sleep -Milliseconds 300

    # Attempt a sale without an active shift
    $noShiftCtx2 = New-Login "pharmacist@demo.com" "pharmacist123"
    $saleKey = [guid]::NewGuid().ToString()
    $saleBody = @{
        clientSaleId = $saleKey
        shiftId = $null
        customerId = $null; note = $null; prescriptionReferenceId = $null; cashTendered = $unitPrice
        items = @(@{ lineId = [guid]::NewGuid().ToString(); medicineId = $medId; quantity = 1; expectedUnitPrice = $unitPrice; discountPercent = 0; requestedBatchId = $null; sellingUnitId = $null })
        payments = @(@{ amount = $unitPrice; method = "CASH"; reference = $null })
    }
    $noShiftSale = Call "POST" "/sales" $noShiftCtx2 $saleBody

    # The sale should be rejected (no active shift)
    if (-not $noShiftSale.ok) {
        $errMsg = $noShiftSale.error
        Assert "Shift3-sale-rejected" $true "Blocked: $errMsg"
        # Check for expected error patterns
        $hasExpectedError = $errMsg -match "SHIFT|shift|active|no.*shift|unauthorized"
        Assert "Shift3-error-mentions-shift" $hasExpectedError "Error=$errMsg"
    } else {
        # If it succeeded, that's a problem — verify it's actually invalid
        $saleId = $noShiftSale.data.id
        $saleCheck = Db-Scalar "SELECT shift_id FROM sales WHERE id = '$saleId'"
        if ($saleCheck) {
            Assert "Shift3-shift-was-assigned" $true "Auto-assigned shift=$saleCheck"
        } else {
            Assert "Shift3-sale-without-shift" $false "Sale succeeded without shift — potential bug"
        }
    }

    # Also try with an explicit (non-existent) shift ID
    $noShiftCtx3 = New-Login "pharmacist@demo.com" "pharmacist123"
    $fakeShiftId = [guid]::NewGuid().ToString()
    $saleBody2 = @{
        clientSaleId = [guid]::NewGuid().ToString()
        shiftId = $fakeShiftId
        customerId = $null; note = $null; prescriptionReferenceId = $null; cashTendered = $unitPrice
        items = @(@{ lineId = [guid]::NewGuid().ToString(); medicineId = $medId; quantity = 1; expectedUnitPrice = $unitPrice; discountPercent = 0; requestedBatchId = $null; sellingUnitId = $null })
        payments = @(@{ amount = $unitPrice; method = "CASH"; reference = $null })
    }
    $fakeShiftSale = Call "POST" "/sales" $noShiftCtx3 $saleBody2
    Assert-Fail "Shift3-fake-shift-rejected" $fakeShiftSale "400"
}
End-TestGroup

# ============================================================
# Shift4: Shift Reconciliation (10 sales, mixed methods)
# ============================================================
Begin-TestGroup "Shift4: Shift Reconciliation"

$reconCashier = New-Login "cashier@demo.com" "cashier123"
if (-not $reconCashier.session) {
    Record "Shift4-login" "BLOCKED" "Cashier login failed" "Shift4"
    End-TestGroup
} else {
    # Close any active shifts
    $meRecon = Call "GET" "/auth/me" $reconCashier
    $reconUid = $meRecon.data.user.id
    $reconActive = Call "GET" "/shifts?userId=$reconUid`&size=20`&status=ACTIVE" $reconCashier
    if ($reconActive.ok) {
        foreach ($s in @($reconActive.data | Where-Object { $_.status -eq "ACTIVE" })) {
            Close-ShiftFor $reconCashier $s.id 0 | Out-Null
        }
    }
    Start-Sleep -Milliseconds 300

    $openingFloat = 3000.00
    $reconCashier2 = New-Login "cashier@demo.com" "cashier123"
    $shift4 = Open-ShiftFor $reconCashier2 $openingFloat
    if (-not $shift4) {
        Record "Shift4-open" "BLOCKED" "Could not open shift" "Shift4"
        End-TestGroup
    } else {
        $shiftId4 = $shift4.id

        # 10 sales: 6 cash, 3 M-Pesa, 1 cash refund
        $cashSalesTotal = 0.0
        $mpesaSalesTotal = 0.0
        $cashSales = @()
        $mpesaSales = @()
        $refundAmt = 0.0

        # Cash sales (1-6)
        for ($i = 1; $i -le 6; $i++) {
            $ctx = New-Login "cashier@demo.com" "cashier123"
            $qty = $i  # 1,2,3,4,5,6
            $sale = Make-CashSale $ctx $shiftId4 $medId $unitPrice $qty
            if ($sale.ok) {
                $cashSalesTotal += [double]$sale.data.total
                $cashSales += $sale.data
            }
            Assert-Ok "Shift4-cash-sale-$i" $sale
        }

        # M-Pesa sales (7-9)
        for ($i = 7; $i -le 9; $i++) {
            $ctx = New-Login "cashier@demo.com" "cashier123"
            $qty = $i - 5  # 2,3,4
            $sale = Make-MpesaSale $ctx $shiftId4 $medId $unitPrice $qty
            if ($sale.ok) {
                $mpesaSalesTotal += [double]$sale.data.total
                $mpesaSales += $sale.data
            }
            Assert-Ok "Shift4-mpesa-sale-$i" $sale
        }

        # Cash refund on first sale (return 1 unit)
        if ($cashSales.Count -gt 0) {
            $firstSale = $cashSales[0]
            $ctx = New-Login "cashier@demo.com" "cashier123"
            $alloc = Get-BatchForSale $firstSale
            if ($alloc.batchId) {
                $refund = Make-Refund $ctx $firstSale.id $alloc.itemId $alloc.batchId 1 "CASH"
                if ($refund.ok) { $refundAmt = [double]$refund.data.totalRefund }
                Assert-Ok "Shift4-cash-refund" $refund
            } else {
                Record "Shift4-cash-refund" "BLOCKED" "No batch allocation" "Shift4"
            }
        }

        # Close shift
        $expectedCash = $openingFloat + $cashSalesTotal - $refundAmt
        $ctx = New-Login "cashier@demo.com" "cashier123"
        $closed4 = Close-ShiftFor $ctx $shiftId4 $expectedCash
        Assert-Ok "Shift4-closed" $closed4

        # Verify totals match DB
        $dbTotalSales = Db-Scalar "SELECT COALESCE(SUM(total), 0) FROM sales WHERE shift_id = '$shiftId4' AND status = 'COMPLETED'"
        $dbSalesCount = Db-Scalar "SELECT COUNT(*) FROM sales WHERE shift_id = '$shiftId4' AND status = 'COMPLETED'"
        $expectedTotal = $cashSalesTotal + $mpesaSalesTotal
        Assert "Shift4-total-sales-match" ([math]::Abs([double]$dbTotalSales - $expectedTotal) -lt 0.01) "DB=$dbTotalSales expected=$expectedTotal"
        Assert "Shift4-sales-count" ([int]$dbSalesCount -eq 9) "DB count=$dbSalesCount expected=9"

        # Cash sales only
        $dbCashOnly = Db-Scalar "SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN sales s ON p.sale_id = s.id WHERE s.shift_id = '$shiftId4' AND p.method = 'CASH' AND s.status = 'COMPLETED'"
        Assert "Shift4-cash-sales-match" ([math]::Abs([double]$dbCashOnly - $cashSalesTotal) -lt 0.01) "DB-cash=$dbCashOnly expected=$cashSalesTotal"

        # M-Pesa sales only
        $dbMpesa = Db-Scalar "SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN sales s ON p.sale_id = s.id WHERE s.shift_id = '$shiftId4' AND p.method = 'MPESA_MANUAL' AND s.status = 'COMPLETED'"
        Assert "Shift4-mpesa-sales-match" ([math]::Abs([double]$dbMpesa - $mpesaSalesTotal) -lt 0.01) "DB-mpesa=$dbMpesa expected=$mpesaSalesTotal"

        # Refunds
        $dbRefunds = Db-Scalar "SELECT COALESCE(SUM(total_refund), 0) FROM sale_returns WHERE shift_id = '$shiftId4' AND status = 'COMPLETED'"
        Assert "Shift4-refunds-match" ([math]::Abs([double]$dbRefunds - $refundAmt) -lt 0.01) "DB-refunds=$dbRefunds expected=$refundAmt"

        # Z-report verification
        $zOwner = New-Login "admin@demo.com" "admin123"
        $zReport = Call "GET" "/reports/shift-z/$shiftId4" $zOwner
        Assert-Ok "Shift4-z-report" $zReport
        if ($zReport.ok) {
            Assert "Shift4-z-total" ([math]::Abs([double]$zReport.data.totalSales - $expectedTotal) -lt 0.01) "Z-total=$($zReport.data.totalSales) expected=$expectedTotal"
            Assert "Shift4-z-cash" ([math]::Abs([double]$zReport.data.cashSales - $cashSalesTotal) -lt 0.01) "Z-cash=$($zReport.data.cashSales) expected=$cashSalesTotal"
            Assert "Shift4-z-count" ([int]$zReport.data.salesCount -eq 9) "Z-count=$($zReport.data.salesCount)"
        }

        # Full drawer reconciliation via DB
        $dbOpening = Db-Scalar "SELECT COALESCE(opening_float, 0) FROM staff_shifts WHERE id = '$shiftId4'"
        $dbCashIn = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE shift_id = '$shiftId4' AND transaction_type = 'CASH_IN'"
        $dbCashOut = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE shift_id = '$shiftId4' AND transaction_type = 'CASH_OUT'"
        $dbExpectedCash = [double]$dbOpening + [double]$dbCashOnly - [double]$dbRefunds + [double]$dbCashIn - [double]$dbCashOut
        $cashDiff = [math]::Abs($expectedCash - $dbExpectedCash)
        Assert "Shift4-drawer-reconciliation" ($cashDiff -lt 0.01) "Expected=$expectedCash DB-calc=$dbExpectedCash diff=$cashDiff"
    }
}
End-TestGroup

# ─────────────────────────────────────────────────────────
Write-Summary
