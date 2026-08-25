# End-to-end smoke suite against a freshly seeded stack.
# Usage: powershell -File scripts\smoke-test.ps1
$ErrorActionPreference = "Continue"
$ApiBase = "http://localhost:9090"
$script:pass = 0
$script:fail = 0
$script:failures = @()

function Ok($name)  { $script:pass++; Write-Host ("  PASS " + $name) }
function Bad($name, $detail) { $script:fail++; $script:failures += $name; Write-Host ("  FAIL " + $name + " :: " + $detail) }

function New-Login($email, $pass) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $c = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
        -Body ('{"email":"' + $email + '","password":"' + $pass + '"}') `
        -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName = $c.token } | Out-Null
    $c2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    return @{ session = $s; headers = @{ $c2.headerName = $c2.token; "Idempotency-Key" = [guid]::NewGuid().ToString() } }
}

function Call($method, $path, $ctx, $body) {
    $params = @{
        Uri = "$ApiBase/api/v1$path"
        Method = $method
        WebSession = $ctx.session
        Headers = $ctx.headers
    }
    if ($body) {
        $params.ContentType = "application/json"
        $params.Body = $body | ConvertTo-Json -Depth 10
    }
    try {
        $resp = Invoke-RestMethod @params
        return @{ ok = $true; data = $resp.data }
    } catch {
        $msg = ""
        try { $sr = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream()); $msg = $sr.ReadToEnd() } catch { $msg = $_.Exception.Message }
        return @{ ok = $false; error = $msg }
    }
}

Write-Host "== A. Auth =="
$owner = New-Login "admin@demo.com" "admin123"
if ($owner.session) { Ok "owner login" } else { Bad "owner login" "no session"; exit 1 }
$me = Call "GET" "/auth/me" $owner
$pharmacyId = $me.data.user.pharmacyId
$branchId   = $me.data.user.activeBranch.id
if ($me.data.user.roles -contains "OWNER") { Ok "me returns OWNER" } else { Bad "me returns OWNER" ($me.data | ConvertTo-Json -Compress) }

Write-Host "== B. Branches & staff =="
$br = Call "POST" "/branches" $owner @{ branchName = "Smoke Branch"; branchCode = "SMK"; phoneNumber = "+254700999888"; location = "Testville"; pharmacyId = $pharmacyId; status = "ACTIVE" }
if ($br.ok) { Ok "branch created" } else { Bad "branch created" $br.error }
$smkBranchId = $br.data.id

$usr = Call "POST" "/users" $owner @{ firstName="Asha"; lastName="Tester"; phoneNumber="+254711999888"; email="asha.smoke@demo.com"; password="staff12345"; branchId=$smkBranchId; status="ACTIVE" }
if ($usr.ok) { Ok "staff registered into Smoke Branch" } else { Bad "staff registered into branch" $usr.error }
$roles = Call "GET" "/roles" $owner
$cashierRole = $roles.data | Where-Object roleName -eq "CASHIER" | Select-Object -First 1
$ubr = Call "POST" "/user-branch-roles" $owner @{ userId = $usr.data.id; branchId = $smkBranchId; roleId = $cashierRole.id }
if ($ubr.ok) { Ok "role assigned in branch" } else { Bad "role assignment" $ubr.error }

Write-Host "== C. Catalog & stock setup =="
powershell -ExecutionPolicy Bypass -File "scripts\seed-demo-data.ps1" *> $null
$meds = Call "GET" "/medicines?size=100&sort=brandName,asc" $owner
if ($meds.data.content.Count -ge 7) { Ok ("medicines seeded (" + $meds.data.content.Count + ")") } else { Bad "medicines seeded" $meds.data.content.Count }
$stock = Call "GET" "/stock?size=1000" $owner
if ($stock.data.content.Count -gt 0) { Ok ("stock rows (" + $stock.data.content.Count + ")") } else { Bad "stock rows" "0" }

Write-Host "== D. Purchase order lifecycle =="
$suppliers = Call "GET" "/suppliers?size=50" $owner
$paracetamol = $meds.data.content | Where-Object brandName -eq "Paracetamol 500mg Tablets" | Select-Object -First 1
$poBody = @{
    supplierId = $suppliers.data.content[0].id
    expectedDeliveryDate = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm:ss")
    items = @(@{ medicineId = $paracetamol.id; quantity = 50; unitCost = 20.00; discount = 0; taxRate = 0 })
}
# adapt to actual DTO names if different
$po = Call "POST" "/purchase-orders" $owner $poBody
if (-not $po.ok) {
    # retry with alternate field casing used by the frontend gateway
    $po = Call "POST" "/purchase-orders" $owner (@{ supplierId = $suppliers.data.content[0].id; expectedDeliveryDate = $null; items = @(@{ medicineId = $paracetamol.id; quantity = 50; buyingPrice = 20.00 }) })
}
if ($po.ok) { Ok "PO created" ; $poId = $po.data.id } else { Bad "PO created" $po.error }
if ($poId) {
    $ap = Call "PATCH" "/purchase-orders/$poId/approve?userId=$($me.data.user.id)" $owner
    if ($ap.ok) { Ok "PO approved" } else { Bad "PO approved" $ap.error }
    $grn = Call "POST" "/goods-received" $owner @{
        supplierId = $suppliers.data.content[0].id
        purchaseOrdersId = $poId
        receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        remarks = "Smoke receive"
        lines = @(@{ medicineId = $paracetamol.id; batchNumber = "SMK-BATCH-" + (Get-Random -Maximum 999); expiryDate = (Get-Date).AddMonths(12).ToString("yyyy-MM-dd"); quantity = 50; unitCost = 20.00; purchaseOrderLineId = $null })
    }
    # GRN requires Idempotency-Key header; Call() already includes one per ctx (static). Refresh:
    $grnCtx = New-Login "admin@demo.com" "admin123"
    $grn = Call "POST" "/goods-received" $grnCtx @{
        supplierId = $suppliers.data.content[0].id
        purchaseOrdersId = $poId
        receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        remarks = "Smoke receive"
        lines = @(@{ medicineId = $paracetamol.id; batchNumber = "SMK-BATCH-" + (Get-Random -Maximum 999); expiryDate = (Get-Date).AddMonths(12).ToString("yyyy-MM-dd"); quantity = 50; unitCost = 20.00; purchaseOrderLineId = $null })
    }
    if ($grn.ok) { Ok "GRN received against PO" } else { Bad "GRN against PO" $grn.error }
}

Write-Host "== E. Shift & sales (as cashier) =="
$cashier = New-Login "cashier@demo.com" "cashier123"
$shift = Call "POST" "/shifts" $cashier @{ openingFloat = 1000; remarks = "start"; shiftName = "Till" }
if ($shift.ok) { Ok "shift opened" ; $shiftId = $shift.data.id } else { Bad "shift opened" $shift.error }

# notification for branch staff?
$notif = Call "GET" ("/notifications?branchId=$branchId&size=10") $cashier
if (($notif.data.content | Where-Object { $_.type -in @("SHIFT_REMINDER","SYSTEM_ALERT") }).Count -ge 1) { Ok "shift-open notification visible" } else { Bad "shift-open notification" "none found" }

# cash out petty cash
$ct = Call "POST" "/cash-transactions" $cashier @{ transactionType = "CASH_OUT"; amount = 100; remarks = "Petty" }
if ($ct.ok) { Ok "cash pay-out recorded" } else { Bad "cash pay-out" $ct.error }

function SellItem($discountPercent, $tendered, $qty) {
    $key = [guid]::NewGuid().ToString()
    $lid = [guid]::NewGuid().ToString()
    $ctx2 = New-Login "cashier@demo.com" "cashier123"
    $ctx2.headers["Idempotency-Key"] = $key
    $body = @{
        clientSaleId = $key; shiftId = $shiftId; customerId = $null; note = $null
        prescriptionReferenceId = $null; cashTendered = $tendered
        items = @(@{ lineId = $lid; medicineId = $paracetamol.id; quantity = $qty; expectedUnitPrice = 40.00; discountPercent = $discountPercent; requestedBatchId = $null; sellingUnitId = $null })
        payments = @(@{ amount = [math]::Round(40.00 * $qty * (1 - $discountPercent / 100), 2); method = "CASH"; reference = $null })
    }
    return Call "POST" "/sales" $ctx2 $body
}

$sale1 = SellItem 0 40.00 1
if ($sale1.ok -and $sale1.data.total -eq 40.00) { Ok "plain sale total 40.00" } else { Bad "plain sale" $sale1.error }
$sale2 = SellItem 10 36.00 1
if ($sale2.ok -and $sale2.data.discountTotal -eq 4.00) { Ok "10% discount sale (discount 4.00)" } else { Bad "discount sale" ($sale2.error) }
$saleMp = New-Login "cashier@demo.com" "cashier123"
$mpKey = [guid]::NewGuid().ToString(); $mpLid = [guid]::NewGuid().ToString()
$saleMp.headers["Idempotency-Key"] = $mpKey
$mp = Call "POST" "/sales" $saleMp @{ clientSaleId = $mpKey; shiftId = $shiftId; customerId = $null; note = $null; prescriptionReferenceId = $null; cashTendered = $null;
    items = @(@{ lineId = $mpLid; medicineId = $paracetamol.id; quantity = 1; expectedUnitPrice = 40.00; requestedBatchId = $null; sellingUnitId = $null });
    payments = @(@{ amount = 40.00; method = "MPESA_MANUAL"; reference = ("SMK" + (Get-Random -Maximum 99999)) }) }
if ($mp.ok) { Ok "m-pesa manual sale" } else { Bad "m-pesa manual sale" $mp.error }

# over-limit discount rejected
$bad = SellItem 90 400.00 10
if (-not $bad.ok) { Ok "over-limit discount rejected" } else { Bad "over-limit discount rejection" "accepted!" }

# return one unit of sale1
$retCtx = New-Login "cashier@demo.com" "cashier123"
$retCtx.headers["Idempotency-Key"] = [guid]::NewGuid().ToString()
$saleDetail = Call "GET" "/sales?size=5&sort=completedAt,desc" $retCtx
$targetSale = $saleDetail.data.content | Where-Object { $_.total -eq 40.00 } | Select-Object -First 1
if ($targetSale) {
    $ret = Call "POST" "/sale-returns" $retCtx @{
        clientReturnId = [guid]::NewGuid().ToString()
        saleId = $targetSale.id
        reason = "Smoke return"
        refundMethod = "CASH"; refundReference = $null
        items = @(@{ medicineBatchesId = $targetSale.items[0].allocations[0].batchId; quantity = 1; saleItemId = $targetSale.items[0].id })
    }
    if ($ret.ok) { Ok "return processed (quarantine)" } else { Bad "return" $ret.error }
} else { Bad "return target sale" "not found" }

Write-Host "== F. Close shift, reconcile, Z =="
$close = Call "PATCH" "/shifts/$shiftId/close" $cashier @{ actualCash = 1076.00; remarks = "counted" }
if ($close.ok) { Ok ("shift closed, variance " + $close.data.variance) } else { Bad "shift closed" $close.error }
$z = Call "GET" "/reports/shift-z/$shiftId" $owner
if ($z.ok) { Ok ("Z report (sales " + $z.data.salesCount + ", cash " + $z.data.totalCashPayments + ")") } else { Bad "Z report" $z.error }
$hist = Call "GET" "/shifts/history" $owner
if ($hist.ok -and $hist.data.Count -ge 1) { Ok ("owner shift history (" + $hist.data.Count + ")") } else { Bad "shift history" "empty" }
$rev = Call "PATCH" "/shifts/$shiftId/variance-review" $owner @{ remarks = "Reviewed in smoke"; status = $null; actualCash = $null }
if ($rev.ok) { Ok "variance reviewed" } else { Bad "variance review" $rev.error }

Write-Host "== G. Reports & notifications =="
$dash = Call "GET" "/reports/dashboard?branchId=$branchId&pharmacyWide=true" $owner
if ($dash.ok) { Ok "dashboard pharmacy-wide" } else { Bad "dashboard pwide" $dash.error }
$inv = Call "GET" "/reports/inventory-summary?branchId=$branchId" $owner
if ($inv.ok) { Ok "inventory summary" } else { Bad "inventory summary" $inv.error }
$sum = Call "GET" "/reports/sales-summary?branchId=$branchId&from=$(Get-Date -Format yyyy-MM-dd)&to=$(Get-Date -Format yyyy-MM-dd)" $owner
if ($sum.ok) { Ok "sales summary" } else { Bad "sales summary" $sum.error }

Write-Host "== H. Expiry write-off & price history =="
$batches = Call "GET" ("/batches?branchId=$branchId&size=100") $owner
$expBatch = $batches.data.content | Where-Object { $_.quantityAvailable -gt 0 } | Select-Object -First 1
if ($expBatch) {
    $wo = Call "POST" "/expiry-logs" $owner @{ medicineBatchesId = $expBatch.id; disposalMethod = "DISPOSAL"; quantityDisposed = 1 }
    if ($wo.ok) { Ok "expiry write-off" } else { Bad "expiry write-off" $wo.error }
}
$ph = Call "GET" "/price-history?medicineId=$($paracetamol.id)" $owner
if ($ph.ok) { Ok ("price history entries (" + $ph.data.Count + ")") } else { Bad "price history" $ph.error }

Write-Host "== I. Prescriptions & dispensing =="
$rx = Call "POST" "/prescriptions" $owner @{
    customerName = "Smoke Patient"; doctorName = "Dr Test"; doctorLicenseNumber = "DL-1"
    hospitalName = ""; prescriptionNumber = "RX-SMOKE-" + (Get-Random -Maximum 9999)
    diagnosis = "Demo"; issuedDate = (Get-Date).ToString("yyyy-MM-dd")
    items = @(@{ medicineId = $paracetamol.id; medicineName = "Paracetamol"; dosage = "1x2"; quantity = 6 })
}
if ($rx.ok) { Ok "prescription created" } else { Bad "prescription created" $rx.error }
if ($rx.ok) {
    $disp = Call "PATCH" "/prescriptions/$($rx.data.id)/dispense" $owner
    if ($disp.ok) { Ok "prescription marked dispensed" } else { Bad "mark dispensed" $disp.error }
}

Write-Host "== J. Terminals pairing =="
$term = Call "POST" "/terminals/register" $owner @{ name = "Smoke Till"; terminalType = "WEB"; branchId = $branchId; platform = "Browser" }
if ($term.ok) {
    Ok "terminal registered"
    $pc = Call "POST" "/terminals/$($term.data.terminalId)/pairing-code" $owner
    if ($pc.ok -and $pc.data.code) {
        Ok ("pairing code generated (" + $pc.data.code + ")")
        $tech = New-Login "technician@demo.com" "tech12345"
        $pair = Call "POST" "/terminals/pair" $tech @{ code = $pc.data.code }
        if ($pair.ok) { Ok "device paired with code" } else { Bad "device pair" $pair.error }
    } else { Bad "pairing code" $pc.error }
} else { Bad "terminal registered" $term.error }

Write-Host "== K. Audit trail =="
$audit = Call "GET" "/audit-logs?size=200&fromDate=$(Get-Date -Format yyyy-MM-dd)" $owner
$actions = @($audit.data.content | ForEach-Object { $_.action })
$needed = @("OPEN_SHIFT","CLOSE_SHIFT","CREATE_PURCHASE_ORDER","APPROVE_PURCHASE_ORDER","CREATE_SALE")
$missingAudit = $needed | Where-Object { $actions -notcontains $_ }
if (-not $missingAudit) { Ok "audit covers shifts/POs/sales" } else { Bad "audit coverage" ($missingAudit -join ",") }

Write-Host ""
Write-Host ("RESULT: {0} passed, {1} failed" -f $script:pass, $script:fail)
if ($script:failures.Count -gt 0) { Write-Host "Failures:"; $script:failures | ForEach-Object { Write-Host (" - " + $_) }; exit 1 }
