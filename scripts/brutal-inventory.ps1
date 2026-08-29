#Requires -Version 5.1
# brutal-inventory.ps1 — Inventory war tests for the Pharmacy POS brutal suite.
# Tests FEFO, stock movements, unit conversions, and return quarantine.
. "$PSScriptRoot\brutal-common.ps1"

Write-Host ""
Write-Host "========================================================" -ForegroundColor White
Write-Host "  BRUTAL INVENTORY WAR TESTS" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor White

# ─────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────

function Open-ShiftFor($ctx, $float) {
    $shift = Call "POST" "/shifts" $ctx @{ openingFloat = $float; remarks = "brutal-inv"; shiftName = "InvTill" }
    if ($shift.ok) { return $shift.data }
    $me = Call "GET" "/auth/me" $ctx
    $uid = $me.data.user.id
    $list = Call "GET" "/shifts?userId=$uid`&size=20" $ctx
    $active = @($list.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
    return $active
}

function Close-ShiftFor($ctx, $shiftId, $actualCash) {
    return Call "PATCH" "/shifts/$shiftId/close" $ctx @{ actualCash = $actualCash; remarks = "counted" }
}

function Make-CashSale($ctx, $shiftId, $medId, $unitPrice, $qty, $batchId) {
    $key = [guid]::NewGuid().ToString()
    $body = @{
        clientSaleId = $key; shiftId = $shiftId; customerId = $null; note = $null
        prescriptionReferenceId = $null; cashTendered = ($unitPrice * $qty)
        items = @(@{ lineId = [guid]::NewGuid().ToString(); medicineId = $medId; quantity = $qty; expectedUnitPrice = $unitPrice; discountPercent = 0; requestedBatchId = $batchId; sellingUnitId = $null })
        payments = @(@{ amount = ($unitPrice * $qty); method = "CASH"; reference = $null })
    }
    return Call "POST" "/sales" $ctx $body
}

function Make-Refund($ctx, $saleId, $saleItemId, $batchId, $qty, $method) {
    $key = [guid]::NewGuid().ToString()
    $body = @{
        clientReturnId = $key; saleId = $saleId; reason = "Brutal inventory return"
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

$owner = New-Login "admin@demo.com" "admin123"
$meds = Call "GET" "/medicines?size=100`&sort=brandName,asc" $owner
if (@($meds.data.content).Count -lt 1) {
    Write-Ts Yellow "No medicines found — seeding..."
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "seed-demo-data.ps1") *> $null
    $owner = New-Login "admin@demo.com" "admin123"
    $meds = Call "GET" "/medicines?size=100`&sort=brandName,asc" $owner
}

# Get a medicine for testing
$paracetamol = @($meds.data.content) | Where-Object { $_.brandName -like "*Paracetamol*" } | Select-Object -First 1
$medId = $paracetamol.id
$unitPrice = [double]$paracetamol.sellingPrice
if ($unitPrice -le 0) { $unitPrice = 40.00 }

# Get supplier for GRN
$suppliers = Call "GET" "/suppliers?size=50" $owner
$supplierId = if ($suppliers.ok -and $suppliers.data.content) { @($suppliers.data.content)[0].id } else { $null }

# Close all active shifts
foreach ($email in @("cashier@demo.com", "admin@demo.com")) {
    $pass = if ($email -eq "admin@demo.com") { "admin123" } else { "cashier123" }
    $ctx = New-Login $email $pass
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
# Inv1: FEFO Verification (First Expiry, First Out)
# ============================================================
Begin-TestGroup "Inv1: FEFO Verification"

$owner = New-Login "admin@demo.com" "admin123"
if (-not $owner.session) {
    Record "Inv1-login" "BLOCKED" "Admin login failed" "Inv1"
    End-TestGroup
} elseif (-not $supplierId) {
    Record "Inv1-setup" "BLOCKED" "No supplier found" "Inv1"
    End-TestGroup
} else {
    # Create a unique test medicine for FEFO testing
    $uid = Get-UniqueId
    $fefoMedName = "FEFO Test Med $uid"
    $fefoMedBody = @{
        brandName = $fefoMedName; genericName = "FEFO Test"; strength = "500mg"
        form = "50000000-0000-0000-0000-000000000001"  # TabletForm
        unitId = "20000000-0000-0000-0000-000000000001"  # TabletUnit
        buyingUnitId = "20000000-0000-0000-0000-000000000007"  # StripUnit
        packSize = 10
        medicineCategoriesId = "10000000-0000-0000-0000-000000000002"  # Analgesics
        manufacturerId = "30000000-0000-0000-0000-000000000002"
        dosageFormId = "50000000-0000-0000-0000-000000000001"
        buyingPrice = 25.00; sellingPrice = 40.00; reorderLevel = 5
        status = "AVAILABLE"; trackSerialNumber = $false; trackBatch = $true; trackExpiry = $true
        requiresPrescription = $false; requiresRefrigeration = $false; isControlledDrug = $false
    }
    $fefoMed = Call "POST" "/medicines" $owner $fefoMedBody
    if (-not $fefoMed.ok) {
        Record "Inv1-create-medicine" "BLOCKED" "Could not create FEFO test medicine: $($fefoMed.error)" "Inv1"
        End-TestGroup
    } else {
        $fefoMedId = $fefoMed.data.id
        Assert-Ok "Inv1-medicine-created" $fefoMed

        # Batch A: near-expiry (30 days), 5 units
        $nearExpiry = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
        $grnA = Call "POST" "/goods-received" $owner @{
            supplierId = $supplierId
            supplierInvoiceNumber = "FEFO-INV-$uid-A"
            purchaseOrdersId = $null
            receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
            remarks = "FEFO near-expiry batch"
            lines = @(@{
                medicineId = $fefoMedId; batchNumber = "FEFO-NEAR-$uid"
                expiryDate = $nearExpiry; quantity = 5; unitCost = 25.00
                purchaseOrderLineId = $null
            })
        }
        Assert-Ok "Inv1-near-batch-grn" $grnA

        # Batch B: far-expiry (24 months), 50 units
        $farExpiry = (Get-Date).AddMonths(24).ToString("yyyy-MM-dd")
        $grnB = Call "POST" "/goods-received" $owner @{
            supplierId = $supplierId
            supplierInvoiceNumber = "FEFO-INV-$uid-B"
            purchaseOrdersId = $null
            receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
            remarks = "FEFO far-expiry batch"
            lines = @(@{
                medicineId = $fefoMedId; batchNumber = "FEFO-FAR-$uid"
                expiryDate = $farExpiry; quantity = 50; unitCost = 25.00
                purchaseOrderLineId = $null
            })
        }
        Assert-Ok "Inv1-far-batch-grn" $grnB

        # Verify stock: 5 + 50 = 55 total
        $totalStock = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$fefoMedId' AND branch_id = '$branchId' AND quantity_available > 0"
        Assert "Inv1-total-stock-55" ([int]$totalStock -eq 55) "Stock=$totalStock expected=55"

        # Get batch IDs
        $nearBatchId = Db-Scalar "SELECT id FROM medicine_batches WHERE medicine_id = '$fefoMedId' AND branch_id = '$branchId' AND batch_number = 'FEFO-NEAR-$uid' LIMIT 1"
        $farBatchId = Db-Scalar "SELECT id FROM medicine_batches WHERE medicine_id = '$fefoMedId' AND branch_id = '$branchId' AND batch_number = 'FEFO-FAR-$uid' LIMIT 1"

        # Open shift for sales
        $cashier = New-Login "cashier@demo.com" "cashier123"
        $shift = Open-ShiftFor $cashier 5000.00
        if (-not $shift) {
            Record "Inv1-shift" "BLOCKED" "Could not open shift" "Inv1"
            End-TestGroup
        } else {
            $shiftId = $shift.id

            # Sell 6 units — should consume all 5 from near-expiry + 1 from far-expiry (FEFO)
            $cashier = New-Login "cashier@demo.com" "cashier123"
            $sale = Make-CashSale $cashier $shiftId $fefoMedId $unitPrice 6 $null
            Assert-Ok "Inv1-sale-6-units" $sale

            if ($sale.ok) {
                # Verify FEFO: near-expiry batch should be fully consumed
                $nearRemaining = Db-Scalar "SELECT COALESCE(quantity_available, 0) FROM medicine_batches WHERE id = '$nearBatchId'"
                $farRemaining = Db-Scalar "SELECT COALESCE(quantity_available, 0) FROM medicine_batches WHERE id = '$farBatchId'"

                Assert "Inv1-near-batch-depleted" ([int]$nearRemaining -eq 0) "Near batch remaining=$nearRemaining expected=0"
                Assert "Inv1-far-batch-reduced" ([int]$farRemaining -eq 49) "Far batch remaining=$farRemaining expected=49"

                # Total remaining = 49
                $totalRemaining = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$fefoMedId' AND branch_id = '$branchId' AND quantity_available > 0"
                Assert "Inv1-total-remaining-49" ([int]$totalRemaining -eq 49) "Total=$totalRemaining expected=49"

                # Verify batch allocation in the sale
                $allocs = Db-Query "SELECT sba.medicine_batches_id, sba.quantity FROM sale_item_allocations sba JOIN sale_items si ON sba.sale_item_id = si.id WHERE si.sale_id = '$($sale.data.id)' ORDER BY sba.quantity DESC"
                Assert "Inv1-allocation-count" ($allocs.Count -eq 2) "Allocations=$($allocs.Count) expected=2"
            }

            # Close shift
            $cashier = New-Login "cashier@demo.com" "cashier123"
            Close-ShiftFor $cashier $shiftId 10000.00 | Out-Null
        }
    }
}
End-TestGroup

# ============================================================
# Inv2: Stock Movement Journal
# ============================================================
Begin-TestGroup "Inv2: Stock Movement Journal"

$owner = New-Login "admin@demo.com" "admin123"
if (-not $owner.session) {
    Record "Inv2-login" "BLOCKED" "Admin login failed" "Inv2"
    End-TestGroup
} else {
    # Use an existing medicine with stock
    $stockResp = Call "GET" "/stock?size=100" $owner
    $stockRows = @($stockResp.data.content) | Where-Object { $_.quantityAvailable -gt 5 }
    if ($stockRows.Count -eq 0) {
        Record "Inv2-setup" "BLOCKED" "No medicine with sufficient stock" "Inv2"
        End-TestGroup
    } else {
        $testStock = $stockRows[0]
        $testMedId = $testStock.medicineId
        $testBatchId = $null
        if ($testStock.medicineBatchesId) { $testBatchId = $testStock.medicineBatchesId }
        elseif ($testStock.batchId) { $testBatchId = $testStock.batchId }
        $testPrice = if ($testStock.sellingPrice) { [double]$testStock.sellingPrice } else { $unitPrice }

        # Record initial stock
        $initialStock = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND quantity_available > 0"
        $initialMovements = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId'"

        # 1. Sale — reduce stock by 2
        $cashier = New-Login "cashier@demo.com" "cashier123"
        $shift = Open-ShiftFor $cashier 5000.00
        $shiftId = if ($shift) { $shift.id } else { $null }
        if ($shiftId) {
            $cashier = New-Login "cashier@demo.com" "cashier123"
            $sale = Make-CashSale $cashier $shiftId $testMedId $testPrice 2 $testBatchId
            Assert-Ok "Inv2-sale-created" $sale

            # Verify movement record for SALE
            if ($sale.ok) {
                $saleMovement = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type = 'SALE' AND reference_id = '$($sale.data.id)'"
                Assert "Inv2-sale-movement-exists" ([int]$saleMovement -ge 1) "Sale movements=$saleMovement"

                $saleQty = Db-Scalar "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type = 'SALE' AND reference_id = '$($sale.data.id)'"
                Assert "Inv2-sale-movement-qty" ([int]$saleQty -eq 2) "Sale qty=$saleQty expected=2"
            }

            # 2. Return — increase stock (goes to quarantine, but movement recorded)
            if ($sale.ok) {
                $cashier = New-Login "cashier@demo.com" "cashier123"
                $alloc = Get-BatchForSale $sale.data
                if ($alloc.batchId) {
                    $refund = Make-Refund $cashier $sale.data.id $alloc.itemId $alloc.batchId 1 "CASH"
                    Assert-Ok "Inv2-return-created" $refund

                    if ($refund.ok) {
                        $returnMovement = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type = 'RETURN' AND reference_id = '$($refund.data.id)'"
                        Assert "Inv2-return-movement-exists" ([int]$returnMovement -ge 1) "Return movements=$returnMovement"
                    }
                }
            }

            # Close shift
            $cashier = New-Login "cashier@demo.com" "cashier123"
            Close-ShiftFor $cashier $shiftId 5000.00 | Out-Null
        }

        # 3. GRN — increase stock
        if ($supplierId) {
            $owner = New-Login "admin@demo.com" "admin123"
            $uid = Get-UniqueId
            $grn = Call "POST" "/goods-received" $owner @{
                supplierId = $supplierId
                supplierInvoiceNumber = "MOV-INV-$uid"
                purchaseOrdersId = $null
                receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
                remarks = "Stock movement test GRN"
                lines = @(@{
                    medicineId = $testMedId; batchNumber = "MOV-$uid"
                    expiryDate = (Get-Date).AddMonths(12).ToString("yyyy-MM-dd")
                    quantity = 10; unitCost = 25.00; purchaseOrderLineId = $null
                })
            }
            Assert-Ok "Inv2-grn-created" $grn

            if ($grn.ok) {
                $grnMovement = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type = 'GRN' AND reference_id = '$($grn.data.id)'"
                Assert "Inv2-grn-movement-exists" ([int]$grnMovement -ge 1) "GRN movements=$grnMovement"
            }
        }

        # 4. Write-off — reduce stock
        $owner = New-Login "admin@demo.com" "admin123"
        $batches = Call "GET" "/batches?branchId=$branchId`&size=100" $owner
        $writeOffBatch = @($batches.data.content) | Where-Object { $_.medicineId -eq $testMedId -and $_.quantityAvailable -gt 0 } | Select-Object -First 1
        if ($writeOffBatch) {
            $writeOff = Call "POST" "/expiry-logs" $owner @{
                medicineBatchesId = $writeOffBatch.id
                disposalMethod = "DISPOSAL"
                quantityDisposed = 1
            }
            Assert-Ok "Inv2-writeoff-created" $writeOff

            if ($writeOff.ok) {
                $writeOffMovement = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type = 'WRITE_OFF'"
                Assert "Inv2-writeoff-movement-exists" ([int]$writeOffMovement -ge 1) "Write-off movements=$writeOffMovement"
            }
        }

        # Verify: sum of movements matches stock balance change
        $finalStock = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND quantity_available > 0"
        $stockChange = [int]$finalStock - [int]$initialStock

        # Sum all positive movements (GRN, RETURN) and negative movements (SALE, WRITE_OFF)
        $positiveMovements = Db-Scalar "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type IN ('GRN', 'RETURN', 'ADJUSTMENT_IN') AND created_at >= (NOW() - INTERVAL '5 minutes')"
        $negativeMovements = Db-Scalar "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId' AND movement_type IN ('SALE', 'WRITE_OFF', 'ADJUSTMENT_OUT') AND created_at >= (NOW() - INTERVAL '5 minutes')"

        $expectedChange = [int]$positiveMovements - [int]$negativeMovements
        Assert "Inv2-movements-balance" ($stockChange -eq $expectedChange) "Stock change=$stockChange movements net=$expectedChange"

        # Verify new movements were created (count increased)
        $finalMovements = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$testMedId' AND branch_id = '$branchId'"
        Assert "Inv2-movements-increased" ([int]$finalMovements -gt [int]$initialMovements) "Initial=$initialMovements Final=$finalMovements"
    }
}
End-TestGroup

# ============================================================
# Inv3: Unit Conversion
# ============================================================
Begin-TestGroup "Inv3: Unit Conversion"

$owner = New-Login "admin@demo.com" "admin123"
if (-not $owner.session) {
    Record "Inv3-login" "BLOCKED" "Admin login failed" "Inv3"
    End-TestGroup
} elseif (-not $supplierId) {
    Record "Inv3-setup" "BLOCKED" "No supplier found" "Inv3"
    End-TestGroup
} else {
    # Create a medicine with Box=10 Strips, Strip=10 Tablets
    # Base unit = Tablet. 1 Box = 10 Strips = 100 Tablets
    $uid = Get-UniqueId
    $convMedName = "Conv Test Med $uid"

    # Reference IDs
    $tabletUnitId = "20000000-0000-0000-0000-000000000001"
    $stripUnitId = "20000000-0000-0000-0000-000000000007"
    $boxUnitId = "20000000-0000-0000-0000-000000000008"
    $tabletFormId = "50000000-0000-0000-0000-000000000001"
    $analgesicsCatId = "10000000-0000-0000-0000-000000000002"
    $manufacturerId = "30000000-0000-0000-0000-000000000002"

    $convMed = Call "POST" "/medicines" $owner @{
        brandName = $convMedName; genericName = "Conv Test"; strength = "500mg"
        form = $tabletFormId; unitId = $tabletUnitId; buyingUnitId = $boxUnitId
        packSize = 100  # 1 Box = 100 Tablets (10 strips * 10 tablets)
        medicineCategoriesId = $analgesicsCatId; manufacturerId = $manufacturerId
        dosageFormId = $tabletFormId
        buyingPrice = 200.00; sellingPrice = 5.00  # Per tablet
        reorderLevel = 50; status = "AVAILABLE"
        trackSerialNumber = $false; trackBatch = $true; trackExpiry = $true
        requiresPrescription = $false; requiresRefrigeration = $false; isControlledDrug = $false
    }
    if (-not $convMed.ok) {
        Record "Inv3-create-medicine" "BLOCKED" "Could not create: $($convMed.error)" "Inv3"
        End-TestGroup
    } else {
        $convMedId = $convMed.data.id
        Assert-Ok "Inv3-medicine-created" $convMed

        # Purchase 2 Boxes = 200 Tablets in base units
        $owner = New-Login "admin@demo.com" "admin123"
        $grnConv = Call "POST" "/goods-received" $owner @{
            supplierId = $supplierId
            supplierInvoiceNumber = "CONV-INV-$uid"
            purchaseOrdersId = $null
            receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
            remarks = "Unit conversion test GRN"
            lines = @(@{
                medicineId = $convMedId; batchNumber = "CONV-$uid"
                expiryDate = (Get-Date).AddMonths(18).ToString("yyyy-MM-dd")
                quantity = 200  # 2 boxes in base units (tablets)
                unitCost = 2.00  # Per tablet
                purchaseOrderLineId = $null
            })
        }
        Assert-Ok "Inv3-grn-200-tablets" $grnConv

        # Verify: stock = 200 Tablets
        $stockAfterPurchase = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$convMedId' AND branch_id = '$branchId' AND quantity_available > 0"
        Assert "Inv3-stock-after-purchase" ([int]$stockAfterPurchase -eq 200) "Stock=$stockAfterPurchase expected=200"

        # Open shift for sales
        $cashier = New-Login "cashier@demo.com" "cashier123"
        $shift = Open-ShiftFor $cashier 5000.00
        $shiftId = if ($shift) { $shift.id } else { $null }
        if (-not $shiftId) {
            Record "Inv3-shift" "BLOCKED" "Could not open shift" "Inv3"
            End-TestGroup
        } else {
            # Sell 3 Strips = 30 Tablets
            $cashier = New-Login "cashier@demo.com" "cashier123"
            $saleStrips = Make-CashSale $cashier $shiftId $convMedId 5.00 30 $null
            Assert-Ok "Inv3-sell-30-tablets" $saleStrips

            # Sell 7 individual Tablets
            $cashier = New-Login "cashier@demo.com" "cashier123"
            $saleTablets = Make-CashSale $cashier $shiftId $convMedId 5.00 7 $null
            Assert-Ok "Inv3-sell-7-tablets" $saleTablets

            # Return 1 Strip = 10 Tablets
            if ($saleStrips.ok) {
                $cashier = New-Login "cashier@demo.com" "cashier123"
                $alloc = Get-BatchForSale $saleStrips.data
                if ($alloc.batchId) {
                    $returnStrip = Make-Refund $cashier $saleStrips.data.id $alloc.itemId $alloc.batchId 10 "CASH"
                    Assert-Ok "Inv3-return-10-tablets" $returnStrip
                }
            }

            # Verify: stock = 200 - 30 - 7 + 10 = 173 Tablets
            # Note: returned tablets go to quarantine, so available = 200 - 30 - 7 = 163
            # quarantined = 10, total = 173
            $stockAfterAll = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$convMedId' AND branch_id = '$branchId'"
            $quarantineAfter = Db-Scalar "SELECT COALESCE(SUM(quantity_quarantined), 0) FROM medicine_batches WHERE medicine_id = '$convMedId' AND branch_id = '$branchId'"
            $totalAfter = [int]$stockAfterAll + [int]$quarantineAfter

            Assert "Inv3-available-stock" ([int]$stockAfterAll -eq 163) "Available=$stockAfterAll expected=163"
            Assert "Inv3-quarantine-stock" ([int]$quarantineAfter -eq 10) "Quarantine=$quarantineAfter expected=10"
            Assert "Inv3-total-stock-173" ($totalAfter -eq 173) "Total=$totalAfter expected=173"

            # DB verification
            $dbAvailable = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$convMedId' AND branch_id = '$branchId' AND quantity_available > 0"
            Assert "Inv3-db-verification" ([int]$dbAvailable -eq 163) "DB available=$dbAvailable expected=163"

            # Close shift
            $cashier = New-Login "cashier@demo.com" "cashier123"
            Close-ShiftFor $cashier $shiftId 10000.00 | Out-Null
        }
    }
}
End-TestGroup

# ============================================================
# Inv4: Return Quarantine
# ============================================================
Begin-TestGroup "Inv4: Return Quarantine"

$owner = New-Login "admin@demo.com" "admin123"
if (-not $owner.session) {
    Record "Inv4-login" "BLOCKED" "Admin login failed" "Inv4"
    End-TestGroup
} else {
    # Use the FEFO test medicine if it exists, or any medicine with stock
    $stockResp = Call "GET" "/stock?size=100" $owner
    $testMed = @($stockResp.data.content) | Where-Object { $_.quantityAvailable -gt 10 } | Select-Object -First 1
    if (-not $testMed) {
        Record "Inv4-setup" "BLOCKED" "No medicine with sufficient stock" "Inv4"
        End-TestGroup
    } else {
        $qMedId = $testMed.medicineId
        $qPrice = if ($testMed.sellingPrice) { [double]$testMed.sellingPrice } else { 40.00 }

        # Record initial state
        $initialAvailable = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$qMedId' AND branch_id = '$branchId' AND quantity_available > 0"
        $initialQuarantine = Db-Scalar "SELECT COALESCE(SUM(quantity_quarantined), 0) FROM medicine_batches WHERE medicine_id = '$qMedId' AND branch_id = '$branchId'"

        # Open shift
        $cashier = New-Login "cashier@demo.com" "cashier123"
        $shift = Open-ShiftFor $cashier 5000.00
        $shiftId = if ($shift) { $shift.id } else { $null }
        if (-not $shiftId) {
            Record "Inv4-shift" "BLOCKED" "Could not open shift" "Inv4"
            End-TestGroup
        } else {
            # Make a sale of 5 units
            $cashier = New-Login "cashier@demo.com" "cashier123"
            $sale = Make-CashSale $cashier $shiftId $qMedId $qPrice 5 $null
            Assert-Ok "Inv4-sale-5-units" $sale

            $availableAfterSale = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$qMedId' AND branch_id = '$branchId' AND quantity_available > 0"
            Assert "Inv4-stock-reduced" ([int]$availableAfterSale -lt [int]$initialAvailable) "Before=$initialAvailable After=$availableAfterSale"

            # Return 3 units — should go to quarantine, NOT back to available
            if ($sale.ok) {
                $cashier = New-Login "cashier@demo.com" "cashier123"
                $alloc = Get-BatchForSale $sale.data
                if ($alloc.batchId) {
                    $return = Make-Refund $cashier $sale.data.id $alloc.itemId $alloc.batchId 3 "CASH"
                    Assert-Ok "Inv4-return-3-units" $return

                    if ($return.ok) {
                        # Verify: returned stock goes to quarantine
                        $availableAfterReturn = Db-Scalar "SELECT COALESCE(SUM(quantity_available), 0) FROM medicine_batches WHERE medicine_id = '$qMedId' AND branch_id = '$branchId' AND quantity_available > 0"
                        $quarantineAfterReturn = Db-Scalar "SELECT COALESCE(SUM(quantity_quarantined), 0) FROM medicine_batches WHERE medicine_id = '$qMedId' AND branch_id = '$branchId'"

                        # quantityAvailable should be UNCHANGED (returned items go to quarantine)
                        Assert "Inv4-available-unchanged" ([int]$availableAfterReturn -eq [int]$availableAfterSale) "Available after sale=$availableAfterSale after return=$availableAfterReturn"

                        # quantityQuarantined should increase by 3
                        $expectedQuarantine = [int]$initialQuarantine + 3
                        Assert "Inv4-quarantine-increased" ([int]$quarantineAfterReturn -ge $expectedQuarantine) "Quarantine=$quarantineAfterReturn expected>=$expectedQuarantine"

                        # Verify the specific batch has quarantine stock
                        $batchQuarantine = Db-Scalar "SELECT COALESCE(quantity_quarantined, 0) FROM medicine_batches WHERE id = '$($alloc.batchId)'"
                        Assert "Inv4-batch-quarantine" ([int]$batchQuarantine -ge 3) "Batch quarantine=$batchQuarantine expected>=3"

                        # Verify via API: stock endpoint should show unchanged available
                        $owner = New-Login "admin@demo.com" "admin123"
                        $stockApi = Call "GET" "/stock?size=100" $owner
                        if ($stockApi.ok) {
                            $apiStock = @($stockApi.data.content) | Where-Object { $_.medicineId -eq $qMedId }
                            $apiAvailable = 0
                            foreach ($s in $apiStock) { $apiAvailable += [int]$s.quantityAvailable }
                            Assert "Inv4-api-available-unchanged" ($apiAvailable -eq [int]$availableAfterSale) "API available=$apiAvailable expected=$availableAfterSale"
                        }

                        # Verify stock_movements journal has RETURN entry
                        $returnMovement = Db-Scalar "SELECT COUNT(*) FROM stock_movements WHERE medicine_id = '$qMedId' AND branch_id = '$branchId' AND movement_type = 'RETURN' AND reference_id = '$($return.data.id)'"
                        Assert "Inv4-return-movement-recorded" ([int]$returnMovement -ge 1) "Return movements=$returnMovement"
                    }
                } else {
                    Record "Inv4-return" "BLOCKED" "No batch allocation found" "Inv4"
                }
            }

            # Close shift
            $cashier = New-Login "cashier@demo.com" "cashier123"
            Close-ShiftFor $cashier $shiftId 10000.00 | Out-Null
        }
    }
}
End-TestGroup

# ─────────────────────────────────────────────────────────
Write-Summary
