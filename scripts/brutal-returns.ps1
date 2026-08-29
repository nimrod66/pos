#Requires -Version 5.1
# brutal-returns.ps1 — Returns from hell tests.
# Sources brutal-common.ps1 for shared utilities.
. "$PSScriptRoot\brutal-common.ps1"

Write-Host ""
Write-Host "========================================================" -ForegroundColor White
Write-Host "  BRUTAL RETURNS FROM HELL TESTS" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor White

# ─────────────────────────────────────────────────────────
# Helper: Create a multi-item sale
# ─────────────────────────────────────────────────────────
function New-MultiItemSale($ctx, $shiftId, $items) {
    $saleKey = [guid]::NewGuid().ToString()
    $ctx.headers["Idempotency-Key"] = $saleKey

    $saleItems = @()
    $totalAmount = 0.0
    foreach ($item in $items) {
        $lineId = [guid]::NewGuid().ToString()
        $lineTotal = $item.price * $item.qty
        $totalAmount += $lineTotal
        $saleItems += @{
            lineId = $lineId
            medicineId = $item.medicineId
            quantity = $item.qty
            expectedUnitPrice = $item.price
            discountPercent = 0
            requestedBatchId = $null
            sellingUnitId = $null
        }
    }

    $body = @{
        clientSaleId = $saleKey
        shiftId = $shiftId
        customerId = $null
        note = "Brutal returns test"
        prescriptionReferenceId = $null
        cashTendered = $totalAmount
        items = $saleItems
        payments = @(@{ amount = $totalAmount; method = "CASH"; reference = $null })
    }

    return Call "POST" "/sales" $ctx $body
}

# Get reference data
$ownerCtx = New-Login "admin@demo.com" "admin123"
if (-not $ownerCtx.session) {
    Write-Host "FATAL: Cannot login as owner" -ForegroundColor Red
    exit 1
}
$me = Call "GET" "/auth/me" $ownerCtx
$branchId = $me.data.user.activeBranch.id
$pharmacyId = $me.data.user.pharmacyId

$meds = Call "GET" "/medicines?size=100&sort=brandName,asc" $ownerCtx
$allMeds = @($meds.data.content)

# Pick 3 different medicines
$medA = $allMeds | Where-Object { $_.brandName -like "*Paracetamol*" } | Select-Object -First 1
$medB = $allMeds | Where-Object { $_.brandName -like "*Amoxicillin*" } | Select-Object -First 1
$medC = $allMeds | Where-Object { $_.brandName -like "*Ibuprofen*" } | Select-Object -First 1

if (-not $medA -or -not $medB -or -not $medC) {
    Write-Host "FATAL: Need Paracetamol, Amoxicillin, and Ibuprofen in catalog" -ForegroundColor Red
    exit 1
}

$priceA = if ($medA.sellingPrice) { [double]$medA.sellingPrice } else { 40.00 }
$priceB = if ($medB.sellingPrice) { [double]$medB.sellingPrice } else { 80.00 }
$priceC = if ($medC.sellingPrice) { [double]$medC.sellingPrice } else { 60.00 }

# Get or create shift for cashier
$cashierCtx = New-Login "cashier@demo.com" "cashier123"
$cashierMe = Call "GET" "/auth/me" $cashierCtx
$cashierUid = $cashierMe.data.user.id
$shift = Call "POST" "/shifts" $cashierCtx @{ openingFloat = 10000; remarks = "Returns test"; shiftName = "Returns Till" }
if ($shift.ok) {
    $shiftId = $shift.data.id
} else {
    $list = Call "GET" "/shifts?userId=$cashierUid&size=20" $cashierCtx
    $activeShift = @($list.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
    if ($activeShift) { $shiftId = $activeShift.id } else {
        Write-Host "FATAL: No active shift" -ForegroundColor Red
        exit 1
    }
}

# ─────────────────────────────────────────────────────────
# Ret1: Multi-Item Return
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Ret1-MultiItemReturn"

$saleCtx = New-Login "cashier@demo.com" "cashier123"
$saleItems = @(
    @{ medicineId = $medA.id; price = $priceA; qty = 3 },
    @{ medicineId = $medB.id; price = $priceB; qty = 2 },
    @{ medicineId = $medC.id; price = $priceC; qty = 4 }
)
$saleResult = New-MultiItemSale $saleCtx $shiftId $saleItems
if (-not $saleResult.ok) {
    Record "Ret1-Create-Sale" "BLOCKED" "Could not create sale: $($saleResult.error)"
    End-TestGroup
} else {
    Record "Ret1-Create-Sale" "PASS" ""
    $saleId = $saleResult.data.id
    $originalTotal = [double]$saleResult.data.total

    $saleDetail = Call "GET" "/sales/$saleId" $saleCtx
    Assert "Ret1-Sale-Has-3-Items" ($saleDetail.ok -and @($saleDetail.data.items).Count -eq 3) "Sale should have 3 items"

    $stockBeforeA = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$($medA.id)' AND branch_id = '$branchId'"
    $stockBeforeB = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$($medB.id)' AND branch_id = '$branchId'"

    $retCtx = New-Login "cashier@demo.com" "cashier123"
    $retKey = [guid]::NewGuid().ToString()
    $retCtx.headers["Idempotency-Key"] = $retKey

    $itemA = $saleDetail.data.items | Where-Object { $_.medicineId -eq $medA.id } | Select-Object -First 1
    $itemB = $saleDetail.data.items | Where-Object { $_.medicineId -eq $medB.id } | Select-Object -First 1

    $allocA = $itemA.allocations | Select-Object -First 1
    $allocB = $itemB.allocations | Select-Object -First 1

    $returnResult = Call "POST" "/sale-returns" $retCtx @{
        clientReturnId = $retKey
        saleId = $saleId
        reason = "Customer changed mind"
        refundMethod = "CASH"
        refundReference = $null
        items = @(
            @{ medicineBatchesId = $allocA.batchId; quantity = 1; saleItemId = $itemA.id },
            @{ medicineBatchesId = $allocB.batchId; quantity = 2; saleItemId = $itemB.id }
        )
    }
    Assert "Ret1-Return-Created" $returnResult.ok "Return should succeed: $($returnResult.error)"

    if ($returnResult.ok) {
        $expectedRefund = $priceA * 1 + $priceB * 2
        $actualRefund = [double]$returnResult.data.refundAmount
        Assert "Ret1-Refund-Amount-Correct" ([math]::Abs($actualRefund - $expectedRefund) -lt 0.01) "Expected refund $expectedRefund, got $actualRefund"

        $updatedSale = Call "GET" "/sales/$saleId" $saleCtx
        Assert "Ret1-Original-Sale-Unchanged" ($updatedSale.ok -and [double]$updatedSale.data.total -eq $originalTotal) "Original sale total should be unchanged"

        $stockAfterA = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$($medA.id)' AND branch_id = '$branchId'"
        $stockAfterB = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$($medB.id)' AND branch_id = '$branchId'"

        $quarantineA = Db-Scalar "SELECT COALESCE(SUM(quantity), 0) FROM return_items ri JOIN sale_returns sr ON ri.return_id = sr.id WHERE sr.sale_id = '$saleId' AND ri.medicine_batches_id = '$($allocA.batchId)'"
        Assert "Ret1-Stock-Quarantined-A" ($quarantineA -ge 1) "Medicine A should have 1 unit quarantined"

        $returnDoc = Db-Scalar "SELECT COUNT(*) FROM sale_returns WHERE sale_id = '$saleId' AND status = 'COMPLETED'"
        Assert "Ret1-Return-Document-Exists" ($returnDoc -ge 1) "Return document should exist in DB"
    }
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Ret2: Over-Return Prevention
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Ret2-OverReturnPrevention"

$saleCtx2 = New-Login "cashier@demo.com" "cashier123"
$saleItems2 = @(
    @{ medicineId = $medA.id; price = $priceA; qty = 5 }
)
$saleResult2 = New-MultiItemSale $saleCtx2 $shiftId $saleItems2
if (-not $saleResult2.ok) {
    Record "Ret2-Create-Sale" "BLOCKED" "Could not create sale: $($saleResult2.error)"
    End-TestGroup
} else {
    Record "Ret2-Create-Sale" "PASS" ""
    $saleId2 = $saleResult2.data.id

    $saleDetail2 = Call "GET" "/sales/$saleId2" $saleCtx2
    $itemToReturn = $saleDetail2.data.items | Where-Object { $_.medicineId -eq $medA.id } | Select-Object -First 1
    $alloc = $itemToReturn.allocations | Select-Object -First 1

    $retCtx2 = New-Login "cashier@demo.com" "cashier123"
    $retKey2 = [guid]::NewGuid().ToString()
    $retCtx2.headers["Idempotency-Key"] = $retKey2
    $ret2a = Call "POST" "/sale-returns" $retCtx2 @{
        clientReturnId = $retKey2
        saleId = $saleId2
        reason = "Partial return"
        refundMethod = "CASH"
        refundReference = $null
        items = @(@{ medicineBatchesId = $alloc.batchId; quantity = 3; saleItemId = $itemToReturn.id })
    }
    Assert "Ret2-First-Return-OK" $ret2a.ok "First return of 3 units should succeed"

    $retCtx3 = New-Login "cashier@demo.com" "cashier123"
    $retKey3 = [guid]::NewGuid().ToString()
    $retCtx3.headers["Idempotency-Key"] = $retKey3
    $ret2b = Call "POST" "/sale-returns" $retCtx3 @{
        clientReturnId = $retKey3
        saleId = $saleId2
        reason = "Attempting over-return"
        refundMethod = "CASH"
        refundReference = $null
        items = @(@{ medicineBatchesId = $alloc.batchId; quantity = 3; saleItemId = $itemToReturn.id })
    }
    $overReturnBlocked = (-not $ret2b.ok) -and ($ret2b.error -match "QUANTITY" -or $ret2b.error -match "exceed" -or $ret2b.status -eq 400)
    Assert "Ret2-Over-Return-Blocked" $overReturnBlocked "Returning 3 more (total 6 > 5) should fail with RETURN_QUANTITY_EXCEEDED"
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Ret3: Return After Shift Closure
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Ret3-ReturnAfterShiftClosure"

$shiftACtx = New-Login "cashier@demo.com" "cashier123"
$shiftAResult = Call "POST" "/shifts" $shiftACtx @{ openingFloat = 5000; remarks = "Shift A"; shiftName = "Shift A" }
if ($shiftAResult.ok) {
    $shiftAId = $shiftAResult.data.id
} else {
    $shiftAList = Call "GET" "/shifts?userId=$cashierUid&size=20" $shiftACtx
    $shiftAActive = @($shiftAList.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
    if ($shiftAActive) { $shiftAId = $shiftAActive.id }
}

if (-not $shiftAId) {
    Record "Ret3-Open-Shift-A" "BLOCKED" "Could not open shift A"
    End-TestGroup
} else {
    Record "Ret3-Open-Shift-A" "PASS" ""

    $saleCtx3 = New-Login "cashier@demo.com" "cashier123"
    $saleItems3 = @(@{ medicineId = $medC.id; price = $priceC; qty = 2 })
    $saleResult3 = New-MultiItemSale $saleCtx3 $shiftAId $saleItems3
    if (-not $saleResult3.ok) {
        Record "Ret3-Create-Sale-In-Shift-A" "BLOCKED" "Could not create sale"
        End-TestGroup
    } else {
        Record "Ret3-Create-Sale-In-Shift-A" "PASS" ""
        $saleId3 = $saleResult3.data.id

        $closeCtx = New-Login "cashier@demo.com" "cashier123"
        $closeResult = Call "PATCH" "/shifts/$shiftAId/close" $closeCtx @{ actualCash = 10000; remarks = "Closing shift A" }
        Assert "Ret3-Shift-A-Closed" $closeResult.ok "Shift A should close successfully"

        $shiftBCtx = New-Login "cashier@demo.com" "cashier123"
        $shiftBResult = Call "POST" "/shifts" $shiftBCtx @{ openingFloat = 5000; remarks = "Shift B"; shiftName = "Shift B" }
        if ($shiftBResult.ok) {
            $shiftBId = $shiftBResult.data.id
        } else {
            $shiftBList = Call "GET" "/shifts?userId=$cashierUid&size=20" $shiftBCtx
            $shiftBActive = @($shiftBList.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
            if ($shiftBActive) { $shiftBId = $shiftBActive.id }
        }
        Assert "Ret3-Shift-B-Opened" ($null -ne $shiftBId) "Shift B should open"

        if ($shiftBId) {
            $retCtx3 = New-Login "cashier@demo.com" "cashier123"
            $retKey3 = [guid]::NewGuid().ToString()
            $retCtx3.headers["Idempotency-Key"] = $retKey3

            $saleDetail3 = Call "GET" "/sales/$saleId3" $retCtx3
            $itemToReturn3 = $saleDetail3.data.items | Select-Object -First 1
            $alloc3 = $itemToReturn3.allocations | Select-Object -First 1

            $ret3 = Call "POST" "/sale-returns" $retCtx3 @{
                clientReturnId = $retKey3
                saleId = $saleId3
                reason = "Return after shift close"
                refundMethod = "CASH"
                refundReference = $null
                items = @(@{ medicineBatchesId = $alloc3.batchId; quantity = 1; saleItemId = $itemToReturn3.id })
            }
            Assert "Ret3-Return-After-Shift-Close-Works" $ret3.ok "Return should work after shift closure"

            if ($ret3.ok) {
                $returnShift = Db-Scalar "SELECT shift_id FROM sale_returns WHERE id = '$($ret3.data.id)'"
                Assert "Ret3-Return-Uses-Current-Shift" ($returnShift -eq $shiftBId) "Return should use current shift B for refund"
            }
        }
    }
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Ret4: Controlled Medicine Return
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Ret4-ControlledMedicineReturn"

$controlledMed = $allMeds | Where-Object { $_.controlled -eq $true -or $_.drugSchedule -ne $null } | Select-Object -First 1
if (-not $controlledMed) {
    $controlledMed = $allMeds | Select-Object -First 1
    Record "Ret4-Find-Controlled-Med" "BLOCKED" "No controlled medicine found, using fallback"
} else {
    Record "Ret4-Find-Controlled-Med" "PASS" ""

    $controlledPrice = if ($controlledMed.sellingPrice) { [double]$controlledMed.sellingPrice } else { 40.00 }

    $saleCtx4 = New-Login "cashier@demo.com" "cashier123"
    $saleItems4 = @(@{ medicineId = $controlledMed.id; price = $controlledPrice; qty = 2 })
    $saleResult4 = New-MultiItemSale $saleCtx4 $shiftId $saleItems4
    if (-not $saleResult4.ok) {
        Record "Ret4-Create-Controlled-Sale" "BLOCKED" "Could not create sale: $($saleResult4.error)"
        End-TestGroup
    } else {
        Record "Ret4-Create-Controlled-Sale" "PASS" ""
        $saleId4 = $saleResult4.data.id

        $saleDetail4 = Call "GET" "/sales/$saleId4" $saleCtx4
        $controlledItem = $saleDetail4.data.items | Where-Object { $_.medicineId -eq $controlledMed.id } | Select-Object -First 1
        $controlledAlloc = $controlledItem.allocations | Select-Object -First 1

        $retCtx4 = New-Login "cashier@demo.com" "cashier123"
        $retKey4 = [guid]::NewGuid().ToString()
        $retCtx4.headers["Idempotency-Key"] = $retKey4
        $ret4 = Call "POST" "/sale-returns" $retCtx4 @{
            clientReturnId = $retKey4
            saleId = $saleId4
            reason = "Controlled medicine return"
            refundMethod = "CASH"
            refundReference = $null
            items = @(@{ medicineBatchesId = $controlledAlloc.batchId; quantity = 1; saleItemId = $controlledItem.id })
        }
        Assert "Ret4-Controlled-Return-Created" $ret4.ok "Controlled medicine return should succeed"

        if ($ret4.ok) {
            $controlledRecord = Db-Scalar "SELECT COUNT(*) FROM controlled_drug_records WHERE sale_return_id = '$($ret4.data.id)'"
            Assert "Ret4-Controlled-Drug-Record-Created" ($controlledRecord -ge 1) "Controlled drug record should be created for return"
        }
    }
}

End-TestGroup

# ─────────────────────────────────────────────────────────
# Output and summary
# ─────────────────────────────────────────────────────────
Write-Summary

$reportPath = Join-Path $PSScriptRoot "brutal-returns-results.json"
Get-TestReport | Out-File -FilePath $reportPath -Encoding UTF8
Write-Host "`nResults written to: $reportPath" -ForegroundColor DarkGray

if ($script:FailCount -gt 0) { exit 1 }
