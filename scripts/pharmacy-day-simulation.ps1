<#
.SYNOPSIS
  Full working day simulation for the pharmacy POS.
  Exercises every major workflow a real pharmacy would hit in a single day.
  Must be run after seed-demo-data has populated the database.
  Outputs pass/fail per step.
#>
$ErrorActionPreference = "Stop"
$ApiBase = "http://localhost:9090"
$script:step = 0
$script:pass = 0
$script:fail = 0

function Step($name)  { $script:step++; Write-Host ("`n=== STEP {0}: {1} ===" -f $script:step, $name) }
function Ok($name)    { $script:pass++; Write-Host ("  PASS  " + $name) }
function Bad($name,$d){ $script:fail++; Write-Host ("  FAIL  " + $name + " :: " + $d) }

function New-Login($email, $pw) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $c = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
        -Body ('{"email":"'+ $email +'","password":"'+ $pw +'"}') `
        -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName=$c.token } | Out-Null
    $c2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    return @{ session=$s; headers=@{ $c2.headerName=$c2.token; "Idempotency-Key"=[guid]::NewGuid().ToString() } }
}

function Call($method,$path,$ctx,$body) {
    $p = @{ Uri="$ApiBase/api/v1$path"; Method=$method; WebSession=$ctx.session; Headers=$ctx.headers }
    if ($body) {
        $p.ContentType = "application/json"
        if ($body -is [string]) { $p.Body=$body } else { $p.Body=$body|ConvertTo-Json -Depth 10 }
    }
    try { $r=Invoke-RestMethod @p; return @{ ok=$true; data=$r.data } }
    catch { $m=""; try { $sr=New-Object IO.StreamReader($_.Exception.Response.GetResponseStream()); $m=$sr.ReadToEnd() } catch { $m=$_.Exception.Message }; return @{ ok=$false; error=$m } }
}

# ── Seed data first if catalog is empty ──
$seedCtx = New-Login "admin@demo.com" "admin123"
$seedCheck = Call "GET" "/medicines?size=1" $seedCtx
if (@($seedCheck.data.content).Count -lt 3) {
    Write-Host "Seeding demo data (first run)..."
    powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "seed-demo-data.ps1") *> $null
}

# ══════════════════════════════════════════════════════════════════════
# STEP 1: Morning - Owner opens the branch, checks dashboard
# ══════════════════════════════════════════════════════════════════════
Step 'Morning - Owner login and dashboard'
$owner = New-Login "admin@demo.com" "admin123"
$me = Call "GET" "/auth/me" $owner
if ($me.ok) { Ok "owner authenticated" } else { Bad "owner login" $me.error }
$pharmacyId = $me.data.user.pharmacyId
$branchId   = $me.data.user.activeBranch.id

$dashUrl = '/reports/dashboard?branchId=' + $branchId + '&pharmacyWide=true'
$dash = Call "GET" $dashUrl $owner
if ($dash.ok) { Ok "dashboard loaded" } else { Bad "dashboard" $dash.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 2: Check low-stock alerts and expiry warnings
# ══════════════════════════════════════════════════════════════════════
Step 'Stock alerts and expiry warnings'
$alertsUrl = '/notifications?branchId=' + $branchId + '&size=20'
$alerts = Call "GET" $alertsUrl $owner
$stockAlerts = @($alerts.data.content | Where-Object { $_.type -eq "LOW_STOCK" })
$expiryAlerts = @($alerts.data.content | Where-Object { $_.type -eq "EXPIRY_WARNING" })
$notifSummary = 'notifications: ' + @($alerts.data.content).Count + ' total, ' + $stockAlerts.Count + ' low-stock, ' + $expiryAlerts.Count + ' expiry'
Ok $notifSummary

# ══════════════════════════════════════════════════════════════════════
# STEP 3: Supplier sends a purchase order
# ══════════════════════════════════════════════════════════════════════
Step 'Purchase order creation and approval'
$suppliers = Call "GET" "/suppliers?size=50" $owner
$medsUrl = '/medicines?size=100&sort=brandName,asc'
$meds = Call "GET" $medsUrl $owner
$paracetamol = $meds.data.content | Where-Object brandName -like "*Paracetamol*" | Select-Object -First 1
$amox = $meds.data.content | Where-Object brandName -like "*Amoxicillin*" | Select-Object -First 1
$ibuprofen = $meds.data.content | Where-Object brandName -like "*Ibuprofen*" | Select-Object -First 1
$ibuprofenPrice = if ($ibuprofen.sellingPrice) { $ibuprofen.sellingPrice } else { 60.00 }
$paracetamolPrice = if ($paracetamol.sellingPrice) { $paracetamol.sellingPrice } else { 40.00 }

$po = Call "POST" "/purchase-orders" $owner @{
    supplierId = $suppliers.data.content[0].id
    branchId = $branchId
    orderedById = $me.data.user.id
    expectedDeliveryDate = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
    items = @(
        @{ medicineId = $paracetamol.id; quantity = 100; buyingPrice = 18.00 },
        @{ medicineId = $amox.id; quantity = 50; buyingPrice = 35.00 }
    )
}
if ($po.ok) { Ok ("PO created with " + @($po.data.items).Count + " lines"); $poId = $po.data.id }
else { Bad "PO created" $po.error; $poId = $null }

if ($poId) {
    $ap = Call "PATCH" ("/purchase-orders/" + $poId + "/approve?userId=" + $me.data.user.id) $owner
    if ($ap.ok) { Ok "PO approved" } else { Bad "PO approved" $ap.error }
}

# ══════════════════════════════════════════════════════════════════════
# STEP 4: GRN - Goods received against PO
# ══════════════════════════════════════════════════════════════════════
Step 'Goods received note'
if ($poId) {
    $grnCtx = New-Login "admin@demo.com" "admin123"
    $poDetail = Call "GET" ("/purchase-orders/" + $poId) $grnCtx
    $items = @($poDetail.data.items)
    $lines = @()
    foreach ($item in $items) {
        $batchNo = "DAY-" + [guid]::NewGuid().ToString().Substring(0,8).ToUpper()
        $lines += @{
            medicineId = $item.medicineId
            batchNumber = $batchNo
            expiryDate = (Get-Date).AddMonths(18).ToString("yyyy-MM-dd")
            quantity = $item.quantity
            unitCost = $item.buyingPrice
            purchaseOrderLineId = $item.id
        }
    }
    $grn = Call "POST" "/goods-received" $grnCtx @{
        supplierId = $suppliers.data.content[0].id
        purchaseOrdersId = $poId
        receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        remarks = "Daily stock delivery"
        lines = $lines
    }
    if ($grn.ok) { Ok ("GRN received, " + $lines.Count + " batches added") } else { Bad "GRN" $grn.error }
}

# ══════════════════════════════════════════════════════════════════════
# STEP 5: Pharmacist creates a prescription
# ══════════════════════════════════════════════════════════════════════
Step 'Prescription creation and dispensing'
$rxUser = New-Login "pharmacist@demo.com" "pharmacist123"
$rx = Call "POST" "/prescriptions" $rxUser @{
    customerName = "John Mwangi"; doctorName = "Dr Wanjiku"; doctorLicenseNumber = "DL-001"
    hospitalName = "Kenyatta Hospital"; prescriptionNumber = ("RX-" + [guid]::NewGuid().ToString().Substring(0,8))
    diagnosis = "Bacterial infection"; issuedDate = (Get-Date).ToString("yyyy-MM-dd")
    items = @(@{ medicineId = $amox.id; medicineName = "Amoxicillin 500mg"; dosage = "1x3"; quantity = 21 })
}
if ($rx.ok) { Ok "prescription created"; $rxId = $rx.data.id } else { Bad "prescription" $rx.error }

if ($rxId) {
    $dispCtx = New-Login "pharmacist@demo.com" "pharmacist123"
    $disp = Call "PATCH" ("/prescriptions/" + $rxId + "/dispense") $dispCtx
    if ($disp.ok) { Ok "prescription dispensed" } else { Bad "dispense" $disp.error }
}

# ══════════════════════════════════════════════════════════════════════
# STEP 6: Cashier opens shift
# ══════════════════════════════════════════════════════════════════════
Step 'Cashier opens shift'
$cashier = New-Login "cashier@demo.com" "cashier123"
$cashierMe = Call "GET" "/auth/me" $cashier
$cashierUid = $cashierMe.data.user.id
$shift = Call "POST" "/shifts" $cashier @{ openingFloat = 5000; remarks = "Morning shift"; shiftName = "Main Till" }
if ($shift.ok) { $shiftId = $shift.data.id; Ok "shift opened, float 5000" }
else {
    $listUrl = '/shifts?userId=' + $cashierUid + '&size=20'
    $list = Call "GET" $listUrl $cashier
    $active = @($list.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
    if ($active) { $shiftId = $active.id; Ok "reusing active shift" }
    else { Bad "shift open" $shift.error; exit 1 }
}

# ══════════════════════════════════════════════════════════════════════
# STEP 7: Cash sale - customer pays with cash
# ══════════════════════════════════════════════════════════════════════
Step 'Cash sale'
$saleCtx = New-Login "cashier@demo.com" "cashier123"
$saleKey = [guid]::NewGuid().ToString()
$saleLid = [guid]::NewGuid().ToString()
$saleCtx.headers["Idempotency-Key"] = $saleKey
$saleTotal = $paracetamolPrice * 2
$sale = Call "POST" "/sales" $saleCtx @{
    clientSaleId = $saleKey; shiftId = $shiftId; customerId = $null; note = "Walk-in customer"
    prescriptionReferenceId = $null; cashTendered = ($saleTotal + 20)
    items = @(@{ lineId = $saleLid; medicineId = $paracetamol.id; quantity = 2; expectedUnitPrice = $paracetamolPrice; discountPercent = 0; requestedBatchId = $null; sellingUnitId = $null })
    payments = @(@{ amount = $saleTotal; method = "CASH"; reference = $null })
}
if ($sale.ok -and $sale.data.total -eq $saleTotal) { Ok ("cash sale total " + $saleTotal) } else { Bad "cash sale" $sale.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 8: M-Pesa sale (Ibuprofen - no prescription required)
# ══════════════════════════════════════════════════════════════════════
Step 'M-Pesa sale'
$mpCtx = New-Login "cashier@demo.com" "cashier123"
$mpKey = [guid]::NewGuid().ToString(); $mpLid = [guid]::NewGuid().ToString()
$mpCtx.headers["Idempotency-Key"] = $mpKey
$mpRef = "MP" + (Get-Random -Maximum 99999).ToString().PadLeft(5,'0')
$mp = Call "POST" "/sales" $mpCtx @{
    clientSaleId = $mpKey; shiftId = $shiftId; customerId = $null; note = "M-Pesa payment"
    prescriptionReferenceId = $null; cashTendered = $null
    items = @(@{ lineId = $mpLid; medicineId = $ibuprofen.id; quantity = 1; expectedUnitPrice = $ibuprofenPrice; discountPercent = 0; requestedBatchId = $null; sellingUnitId = $null })
    payments = @(@{ amount = $ibuprofenPrice; method = "MPESA_MANUAL"; reference = $mpRef })
}
if ($mp.ok) { Ok "M-Pesa sale recorded" } else { Bad "M-Pesa sale" $mp.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 9: Discounted sale (10% line discount)
# ══════════════════════════════════════════════════════════════════════
Step 'Discounted sale'
$discCtx = New-Login "cashier@demo.com" "cashier123"
$discKey = [guid]::NewGuid().ToString(); $discLid = [guid]::NewGuid().ToString()
$discCtx.headers["Idempotency-Key"] = $discKey
$discExpected = [math]::Round($paracetamolPrice * 0.9, 2)
$disc = Call "POST" "/sales" $discCtx @{
    clientSaleId = $discKey; shiftId = $shiftId; customerId = $null; note = "Loyalty discount"
    prescriptionReferenceId = $null; cashTendered = $discExpected
    items = @(@{ lineId = $discLid; medicineId = $paracetamol.id; quantity = 1; expectedUnitPrice = $paracetamolPrice; discountPercent = 10; requestedBatchId = $null; sellingUnitId = $null })
    payments = @(@{ amount = $discExpected; method = "CASH"; reference = $null })
}
$expectedDiscount = [math]::Round($paracetamolPrice * 0.1, 2)
if ($disc.ok -and $disc.data.discountTotal -eq $expectedDiscount) { Ok ("10% discount applied (saved " + $expectedDiscount + ")") } else { Bad "discount sale" $disc.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 10: Cash pay-in and pay-out
# ══════════════════════════════════════════════════════════════════════
Step 'Cash drawer movements'
$ciCtx = New-Login "cashier@demo.com" "cashier123"
$ci = Call "POST" "/cash-transactions" $ciCtx @{ transactionType = "CASH_IN"; amount = 2000; remarks = "Float top-up from safe" }
if ($ci.ok) { Ok "cash-in recorded" } else { Bad "cash-in" $ci.error }

$coCtx = New-Login "cashier@demo.com" "cashier123"
$co = Call "POST" "/cash-transactions" $coCtx @{ transactionType = "CASH_OUT"; amount = 150; remarks = "Petty cash - stationery" }
if ($co.ok) { Ok "cash-out recorded" } else { Bad "cash-out" $co.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 11: Customer returns medicine
# ══════════════════════════════════════════════════════════════════════
Step 'Sale return'
$retCtx = New-Login "cashier@demo.com" "cashier123"
$saleListUrl = '/sales?size=10&sort=completedAt,desc'
$saleList = Call "GET" $saleListUrl $retCtx
$targetSale = $saleList.data.content | Where-Object { $_.total -eq $saleTotal } | Select-Object -First 1
if ($targetSale) {
    $retKey = [guid]::NewGuid().ToString()
    $retCtx.headers["Idempotency-Key"] = $retKey
    $ret = Call "POST" "/sale-returns" $retCtx @{
        clientReturnId = $retKey; saleId = $targetSale.id; reason = "Wrong medication"
        refundMethod = "CASH"; refundReference = $null
        items = @(@{ medicineBatchesId = $targetSale.items[0].allocations[0].batchId; quantity = 1; saleItemId = $targetSale.items[0].id })
    }
    if ($ret.ok) { Ok "return processed (quarantine)" } else { Bad "return" $ret.error }
} else { Bad "return" "target sale not found" }

# ══════════════════════════════════════════════════════════════════════
# STEP 12: Record business expenses
# ══════════════════════════════════════════════════════════════════════
Step 'Record expenses'
$admCtx = New-Login "admin@demo.com" "admin123"
$cat = Call "POST" "/expense-categories" $admCtx @{ categoryName = "Utilities"; categoryDescription = "Electricity, water, internet" }
if ($cat.ok) { $catId = $cat.data.id } else {
    $cats = Call "GET" "/expense-categories?size=50" $admCtx
    $catId = ($cats.data.content | Where-Object { $_.categoryName -eq "Utilities" } | Select-Object -First 1).id
}
$expDate = (Get-Date).ToString("yyyy-MM-dd")
$exp = Call "POST" "/expenses" $admCtx @{ expenseCategoryId = $catId; description = "August electricity bill"; amount = 8500.00; userId = $me.data.user.id; expenseDate = $expDate }
if ($exp.ok) { Ok "expense recorded (KES 8,500)" } else { Bad "expense" $exp.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 13: Insurance claim
# ══════════════════════════════════════════════════════════════════════
Step 'Insurance claim'
$insCtx = New-Login "admin@demo.com" "admin123"
$insurers = Call "GET" "/insurance/insurers?size=10" $insCtx
$insurer = $insurers.data.content | Where-Object { $_.status -eq "ACTIVE" } | Select-Object -First 1
if ($insurer) {
    $insSalesList = Call "GET" '/sales?size=5&sort=completedAt,desc' $insCtx
    $insSale = $insSalesList.data.content | Where-Object { $_.total -gt 0 } | Select-Object -First 1
    if ($insSale) {
        $clmNum = "CLM-" + [guid]::NewGuid().ToString().Substring(0,8)
        $claimAmt = $insSale.total
        $saleIdStr = $insSale.id.ToString()
        $insurerIdStr = $insurer.id.ToString()
        $claimBody = '{' +
            '"insurerId":"' + $insurerIdStr + '",' +
            '"claimNumber":"' + $clmNum + '",' +
            '"patientName":"Mary Njeri",' +
            '"patientNumber":"NHIF-12345",' +
            '"prescribedBy":"Dr Wanjiku",' +
            '"claimAmount":' + $claimAmt + ',' +
            '"saleTotal":' + $insSale.total + ',' +
            '"saleId":"' + $saleIdStr + '"' +
            '}'
        $claim = Call "POST" "/insurance/claims" $insCtx $claimBody
        if ($claim.ok) { Ok "insurance claim created" } else { Bad "insurance claim" $claim.error }
    } else { Ok "skipped (no sales to link)" }
} else { Ok "skipped (no active insurer)" }

# ══════════════════════════════════════════════════════════════════════
# STEP 14: End-of-day reports
# ══════════════════════════════════════════════════════════════════════
Step 'End-of-day reports'
$rptCtx = New-Login "admin@demo.com" "admin123"
$today = Get-Date -Format yyyy-MM-dd
$cashierRptCtx = New-Login "cashier@demo.com" "cashier123"
$cashierMe2 = Call "GET" "/auth/me" $cashierRptCtx
$salesBranchId = $cashierMe2.data.user.activeBranch.id
$ssUrl = '/reports/sales-summary?branchId=' + $salesBranchId + '&from=' + $today + '&to=' + $today
$ss = Call "GET" $ssUrl $rptCtx
if ($ss.ok -and $ss.data.completedSalesCount -ge 1) {
    Ok ("sales summary: " + $ss.data.completedSalesCount + " sales, KES " + $ss.data.netSales)
} else { Bad "sales summary" $ss.error }

$inv = Call "GET" "/reports/inventory-summary?branchId=$branchId" $rptCtx
if ($inv.ok) { Ok "inventory summary fetched" } else { Bad "inventory summary" $inv.error }

$pluUrl = '/reports/plu?branchId=' + $branchId + '&from=' + $today + '&to=' + $today
$plu = Call "GET" $pluUrl $rptCtx
if ($plu.ok) { Ok ("PLU report: " + @($plu.data).Count + " rows") } else { Bad "PLU report" $plu.error }

$auditUrl = '/audit-logs?size=50&fromDate=' + $today
$audit = Call "GET" $auditUrl $rptCtx
if ($audit.ok -and @($audit.data.content).Count -ge 5) { Ok ("audit trail: " + @($audit.data.content).Count + " entries today") } else { Bad "audit trail" $audit.error }

# ══════════════════════════════════════════════════════════════════════
# STEP 15: Cashier closes shift and owner reconciles
# ══════════════════════════════════════════════════════════════════════
Step 'Close shift and reconcile'
$closeCtx = New-Login "cashier@demo.com" "cashier123"
$close = Call "PATCH" ("/shifts/" + $shiftId + "/close") $closeCtx @{ actualCash = 7200.00; remarks = "Counted at close" }
if ($close.ok) { Ok ("shift closed, variance " + $close.data.variance) } else { Bad "shift close" $close.error }

$zCtx = New-Login "admin@demo.com" "admin123"
$z = Call "GET" ("/reports/shift-z/" + $shiftId) $zCtx
if ($z.ok) { Ok ("Z-report: " + $z.data.salesCount + " sales, KES " + $z.data.totalSales) } else { Bad "Z-report" $z.error }

$hist = Call "GET" "/shifts/history" $zCtx
if ($hist.ok -and @($hist.data).Count -ge 1) { Ok "shift history visible to owner" } else { Bad "shift history" $hist.error }

# ══════════════════════════════════════════════════════════════════════
# Summary
# ══════════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host ("=" * 60)
Write-Host ("PHARMACY DAY SIMULATION: {0} passed, {1} failed (of {2} steps)" -f $script:pass, $script:fail, $script:step)
Write-Host ("=" * 60)
if ($script:fail -gt 0) { exit 1 }
