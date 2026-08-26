# End-to-end smoke suite against the running stack. Idempotent: safe to
# re-run against any database state. Each phase uses a fresh login because
# the backend enforces one active session per user.
$ErrorActionPreference = "Continue"
$ApiBase = "http://localhost:9090"
$script:pass = 0
$script:fail = 0
$script:failures = @()
$runTag = (Get-Random -Maximum 9999).ToString("0000")

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
    $params = @{ Uri = "$ApiBase/api/v1$path"; Method = $method; WebSession = $ctx.session; Headers = $ctx.headers }
    if ($body) {
        $params.ContentType = "application/json"
        if ($body -is [string]) { $params.Body = $body } else { $params.Body = $body | ConvertTo-Json -Depth 10 }
    }
    try { $resp = Invoke-RestMethod @params; return @{ ok = $true; data = $resp.data } }
    catch {
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
if ($me.data.user.roles -contains "OWNER") { Ok "me returns OWNER" } else { Bad "me returns OWNER" "" }

Write-Host "== B. Branches & staff =="
$branches = Call "GET" "/branches?pharmacyId=$pharmacyId" $owner
$smk = $branches.data | Where-Object { $_.branchCode -like "SMK*" } | Select-Object -First 1
if (-not $smk) {
    $br = Call "POST" "/branches" $owner @{ branchName = "Smoke Branch"; branchCode = ("SMK" + $runTag); phoneNumber = "+254700999888"; location = "Testville"; pharmacyId = $pharmacyId; status = "ACTIVE" }
    if ($br.ok) { $smk = $br.data } else { Bad "branch created" $br.error }
}
if ($smk) { Ok ("branch available: " + $smk.branchCode); $smkBranchId = $smk.id }

$ashaEmail = ("asha.smoke" + $runTag + "@demo.com")
$usr = Call "POST" "/users" $owner @{ firstName="Asha"; lastName="Tester"; phoneNumber="+25471199" + $runTag; email=$ashaEmail; password="staff12345"; branchId=$smkBranchId; status="ACTIVE" }
if ($usr.ok) { Ok "staff registered into Smoke Branch" } else { Bad "staff registered into branch" $usr.error }
$roles = Call "GET" "/roles" $owner
$cashierRole = $roles.data | Where-Object roleName -eq "CASHIER" | Select-Object -First 1
if ($usr.ok -and $cashierRole) {
    $ubr = Call "POST" "/user-branch-roles" $owner @{ userId = $usr.data.id; branchId = $smkBranchId; roleId = $cashierRole.id }
    if ($ubr.ok) { Ok "role assigned in branch" } else { Bad "role assignment" $ubr.error }
}

Write-Host "== C. Catalog & stock =="
powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "seed-demo-data.ps1") *> $null
# Child logged in as admin -> our session was revoked; re-login.
$owner = New-Login "admin@demo.com" "admin123"
$meds = Call "GET" "/medicines?size=100&sort=brandName,asc" $owner
if (@($meds.data.content).Count -ge 7) { Ok ("medicines seeded (" + @($meds.data.content).Count + ")") } else { Bad "medicines seeded" @($meds.data.content).Count }
$paracetamol = $meds.data.content | Where-Object brandName -eq "Paracetamol 500mg Tablets" | Select-Object -First 1
$stock = Call "GET" "/stock?size=1000" $owner
if (@($stock.data.content).Count -gt 0) { Ok ("stock rows (" + @($stock.data.content).Count + ")") } else { Bad "stock rows" "0" }

Write-Host "== D. Purchase order lifecycle =="
$suppliers = Call "GET" "/suppliers?size=50" $owner
$po = Call "POST" "/purchase-orders" $owner @{
    supplierId = $suppliers.data.content[0].id
    branchId = $branchId
    orderedById = $me.data.user.id
    expectedDeliveryDate = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm:ss")
    items = @(@{ medicineId = $paracetamol.id; quantity = 50; buyingPrice = 20.00 })
}
if ($po.ok) {
    Ok "PO created"; $poId = $po.data.id
    $ap = Call "PATCH" ("/purchase-orders/$poId/approve?userId=" + $me.data.user.id) $owner
    if ($ap.ok) { Ok "PO approved" } else { Bad "PO approved" $ap.error }
    $grnCtx = New-Login "admin@demo.com" "admin123"
    $poDetail = Call "GET" "/purchase-orders/$poId" $grnCtx
    $poLineId = if ($poDetail.data.items -and @($poDetail.data.items).Count -gt 0) { $poDetail.data.items[0].id } else { $null }
    $grn = Call "POST" "/goods-received" $grnCtx @{
        supplierId = $suppliers.data.content[0].id
        purchaseOrdersId = $poId
        receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss"); remarks = "Smoke receive"
        lines = @(@{ medicineId = $paracetamol.id; batchNumber = ("SMK-" + $runTag); expiryDate = (Get-Date).AddMonths(12).ToString("yyyy-MM-dd"); quantity = 50; unitCost = 20.00; purchaseOrderLineId = $poLineId })
    }
    if ($grn.ok) { Ok "GRN received against PO" } else { Bad "GRN against PO" $grn.error }
} else { Bad "PO created" $po.error; $poId = $null }

Write-Host "== E. Shift & sales (fresh cashier login) =="
$cashier = New-Login "cashier@demo.com" "cashier123"
$cashierMe = Call "GET" "/auth/me" $cashier
$cashierUid = $cashierMe.data.user.id
$shift = Call "POST" "/shifts" $cashier @{ openingFloat = 1000; remarks = "start"; shiftName = "Till" }
if ($shift.ok) { $shiftId = $shift.data.id; Ok "shift opened" }
else {
    # 409 = already open from an earlier run; find it
    $list = Call "GET" "/shifts?userId=$cashierUid&size=20" $cashier
    $activeShift = @($list.data | Where-Object { $_.status -eq "ACTIVE" }) | Select-Object -First 1
    if ($activeShift) { $shiftId = $activeShift.id; Ok "reusing open shift" }
    else { Bad "shift opened (and no active found)" $shift.error; exit 1 }
}

$notif = Call "GET" ("/notifications?branchId=" + $branchId + "&size=10") $cashier
if ((@($notif.data.content) | Where-Object { $_.type -in @("SHIFT_REMINDER","SYSTEM_ALERT") }).Count -ge 1) { Ok "shift-open notification visible" } else { Write-Host "  note: no unread shift notification found" }

$ct = Call "POST" "/cash-transactions" $cashier @{ transactionType = "CASH_OUT"; amount = 100; remarks = ("Petty " + $runTag) }
if ($ct.ok) { Ok "cash pay-out recorded" } else { Bad "cash pay-out" $ct.error }

function SellItem($discountPercent, $tendered, $qty) {
    $ctx2 = New-Login "cashier@demo.com" "cashier123"
    $key = [guid]::NewGuid().ToString()
    $lid = [guid]::NewGuid().ToString()
    $ctx2.headers["Idempotency-Key"] = $key
    $net = [math]::Round(40.00 * $qty * (1 - $discountPercent / 100), 2)
    $body = @{
        clientSaleId = $key; shiftId = $shiftId; customerId = $null; note = $null
        prescriptionReferenceId = $null; cashTendered = $tendered
        items = @(@{ lineId = $lid; medicineId = $paracetamol.id; quantity = $qty; expectedUnitPrice = 40.00; discountPercent = $discountPercent; requestedBatchId = $null; sellingUnitId = $null })
        payments = @(@{ amount = $net; method = "CASH"; reference = $null })
    }
    return Call "POST" "/sales" $ctx2 $body
}

$sale1 = SellItem 0 40.00 1
if ($sale1.ok -and $sale1.data.total -eq 40.00) { Ok "plain sale total 40.00" } else { Bad "plain sale" $sale1.error }
$sale2 = SellItem 10 36.00 1
if ($sale2.ok -and $sale2.data.discountTotal -eq 4.00) { Ok "10% discount sale (discount 4.00)" } else { Bad "discount sale" $sale2.error }

$mpCtx = New-Login "cashier@demo.com" "cashier123"
$mpKey = [guid]::NewGuid().ToString(); $mpLid = [guid]::NewGuid().ToString()
$mpCtx.headers["Idempotency-Key"] = $mpKey
$mpBody = @{ clientSaleId = $mpKey; shiftId = $shiftId; customerId = $null; note = $null; prescriptionReferenceId = $null; cashTendered = $null
    items = @(@{ lineId = $mpLid; medicineId = $paracetamol.id; quantity = 1; expectedUnitPrice = 40.00; requestedBatchId = $null; sellingUnitId = $null })
    payments = @(@{ amount = 40.00; method = "MPESA_MANUAL"; reference = ("SMK" + (Get-Random -Maximum 99999)) }) }
$mp = Call "POST" "/sales" $mpCtx $mpBody
if ($mp.ok) { Ok "m-pesa manual sale" } else { Bad "m-pesa manual sale" $mp.error }

$badSale = SellItem 90 360.00 10
if (-not $badSale.ok) { Ok "over-limit discount rejected" } else { Bad "over-limit discount rejection" "accepted!" }

$retCtx = New-Login "cashier@demo.com" "cashier123"
$saleList = Call "GET" "/sales?size=10&sort=completedAt,desc" $retCtx
$targetSale = $saleList.data.content | Where-Object { $_.total -eq 40.00 } | Select-Object -First 1
if ($targetSale) {
    $retKey = [guid]::NewGuid().ToString(); $retCtx.headers["Idempotency-Key"] = $retKey
    $ret = Call "POST" "/sale-returns" $retCtx @{
        clientReturnId = $retKey; saleId = $targetSale.id; reason = "Smoke return"
        refundMethod = "CASH"; refundReference = $null
        items = @(@{ medicineBatchesId = $targetSale.items[0].allocations[0].batchId; quantity = 1; saleItemId = $targetSale.items[0].id })
    }
    if ($ret.ok) { Ok "return processed (quarantine)" } else { Bad "return" $ret.error }
} else { Bad "return target sale" "not found" }

Write-Host "== F. Close, reconcile, Z =="
$cashierClose = New-Login "cashier@demo.com" "cashier123"
$close = Call "PATCH" "/shifts/$shiftId/close" $cashierClose @{ actualCash = 1000.00; remarks = "counted" }
if ($close.ok) { Ok ("shift closed, variance " + $close.data.variance) } else { Bad "shift closed" $close.error }
$zOwner = New-Login "admin@demo.com" "admin123"
$z = Call "GET" "/reports/shift-z/$shiftId" $zOwner
if ($z.ok) { Ok ("Z report (sales " + $z.data.salesCount + ")") } else { Bad "Z report" $z.error }
$hist = Call "GET" "/shifts/history" $zOwner
if ($hist.ok -and @($hist.data).Count -ge 1) { Ok ("owner shift history (" + @($hist.data).Count + ")") } else { Bad "shift history" "empty" }
$revOwner = New-Login "admin@demo.com" "admin123"
$rev = Call "PATCH" "/shifts/$shiftId/variance-review" $revOwner @{ remarks = "Reviewed in smoke"; status = $null; actualCash = $null }
if ($rev.ok) { Ok "variance reviewed" } else { Bad "variance review" $rev.error }

Write-Host "== G. Reports & notifications =="
$gOwner = New-Login "admin@demo.com" "admin123"
$dash = Call "GET" "/reports/dashboard?branchId=$branchId&pharmacyWide=true" $gOwner
if ($dash.ok) { Ok "dashboard pharmacy-wide" } else { Bad "dashboard pwide" $dash.error }
$inv = Call "GET" "/reports/inventory-summary?branchId=$branchId" $gOwner
if ($inv.ok) { Ok "inventory summary" } else { Bad "inventory summary" $inv.error }
$sum = Call "GET" "/reports/sales-summary?branchId=$branchId&from=$(Get-Date -Format yyyy-MM-dd)&to=$(Get-Date -Format yyyy-MM-dd)" $gOwner
if ($sum.ok) { Ok "sales summary" } else { Bad "sales summary" $sum.error }
$plu = Call "GET" "/reports/plu?branchId=$branchId&from=$(Get-Date -Format yyyy-MM-dd)&to=$(Get-Date -Format yyyy-MM-dd)" $gOwner
if ($plu.ok) { Ok ("PLU report (" + @($plu.data).Count + " rows)") } else { Bad "PLU report" $plu.error }

Write-Host "== H. Expiry write-off & price history =="
$hOwner = New-Login "admin@demo.com" "admin123"
$batches = Call "GET" "/batches?branchId=$branchId&size=100" $hOwner
$expBatch = $batches.data.content | Where-Object { $_.quantityAvailable -gt 0 } | Select-Object -First 1
if ($expBatch) {
    $wo = Call "POST" "/expiry-logs" $hOwner @{ medicineBatchesId = $expBatch.id; disposalMethod = "DISPOSAL"; quantityDisposed = 1 }
    if ($wo.ok) { Ok "expiry write-off" } else { Bad "expiry write-off" $wo.error }
}
$ph = Call "GET" "/price-history?medicineId=$($paracetamol.id)" $hOwner
if ($ph.ok) { Ok ("price history entries (" + @($ph.data).Count + ")") } else { Bad "price history" $ph.error }

Write-Host "== I. Prescriptions & dispensing (pharmacist) =="
$rxUser = New-Login "pharmacist@demo.com" "pharmacist123"
$rx = Call "POST" "/prescriptions" $rxUser @{
    customerName = "Smoke Patient"; doctorName = "Dr Test"; doctorLicenseNumber = "DL-1"
    hospitalName = ""; prescriptionNumber = ("RX-SMOKE-" + $runTag)
    diagnosis = "Demo"; issuedDate = (Get-Date).ToString("yyyy-MM-dd")
    items = @(@{ medicineId = $paracetamol.id; medicineName = "Paracetamol"; dosage = "1x2"; quantity = 6 })
}
if ($rx.ok) { Ok "prescription created" } else { Bad "prescription created" $rx.error }
if ($rx.ok) {
    $rxDisp = New-Login "pharmacist@demo.com" "pharmacist123"
    $rc = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $rxDisp.session).data
    $rxDisp.headers[$rc.headerName] = $rc.token
    $disp = Call "PATCH" "/prescriptions/$($rx.data.id)/dispense" $rxDisp
    if ($disp.ok) { Ok "prescription marked dispensed" } else { Bad "mark dispensed" $disp.error }
}

Write-Host "== J. Terminals pairing =="
$jOwner = New-Login "admin@demo.com" "admin123"
$termName = "Smoke Till " + $runTag
$term = Call "POST" "/terminals/register" $jOwner @{ name = $termName; terminalType = "WEB"; branchId = $branchId; platform = "Browser" }
if ($term.ok) {
    Ok "terminal registered"
    $pc = Call "POST" "/terminals/$($term.data.terminalId)/pairing-code" $jOwner
    if ($pc.ok -and $pc.data.code) {
        Ok ("pairing code generated (" + $pc.data.code + ")")
        $tech = New-Login "technician@demo.com" "tech12345"
        $pair = Call "POST" "/terminals/pair" $tech @{ code = $pc.data.code }
        if ($pair.ok) { Ok "device paired with code" } else { Bad "device pair" $pair.error }
    } else { Bad "pairing code" $pc.error }
} else { Bad "terminal registered" $term.error }

Write-Host "== K. Audit trail =="
$kOwner = New-Login "admin@demo.com" "admin123"
$audit = Call "GET" "/audit-logs?size=300&fromDate=$(Get-Date -Format yyyy-MM-dd)" $kOwner
$actions = @($audit.data.content | ForEach-Object { $_.action })
$needed = @("OPEN_SHIFT","CLOSE_SHIFT","CREATE_PURCHASE_ORDER","APPROVE_PURCHASE_ORDER","CREATE_SALE")
$missingAudit = $needed | Where-Object { $actions -notcontains $_ }
if (-not $missingAudit) { Ok "audit covers shifts/POs/sales" } else { Bad "audit coverage" ("missing: " + ($missingAudit -join ",")) }

Write-Host "== L. Medicine SKU & barcode lookup =="
$lOwner = New-Login "admin@demo.com" "admin123"
$skuLookup = Call "GET" "/medicines?size=100&sort=brandName,asc" $lOwner
$skuMatch = $skuLookup.data.content | Where-Object { $_.sku -eq $paracetamol.sku } | Select-Object -First 1
if ($skuMatch) { Ok ("SKU lookup finds " + $paracetamol.sku) } else { Bad "SKU lookup" "sku not in results" }
$barcodeMatch = $skuLookup.data.content | Where-Object { $_.barcode -eq $paracetamol.barcode } | Select-Object -First 1
if ($barcodeMatch) { Ok ("barcode lookup finds " + $paracetamol.barcode) } else { Bad "barcode lookup" "barcode not in results" }
$posLookup = Call "GET" "/pos/lookup?name=$($paracetamol.brandName)" $lOwner
if ($posLookup.ok -and @($posLookup.data).Count -ge 1) { Ok "POS name lookup" } else { Bad "POS name lookup" $posLookup.error }

Write-Host "== M. Expenses =="
$mOwner = New-Login "admin@demo.com" "admin123"
$expCat = Call "POST" "/expense-categories" $mOwner @{ categoryName = ("Smoke Cat " + $runTag); categoryDescription = "Test category" }
if ($expCat.ok) { Ok "expense category created"; $firstCat = $expCat.data } else { Bad "expense category" $expCat.error }
$expCats = Call "GET" "/expense-categories?size=50" $mOwner
if ($expCats.ok) { Ok ("expense categories (" + @($expCats.data.content).Count + ")") } else { Bad "expense categories" $expCats.error }
if ($firstCat) {
    $exp = Call "POST" "/expenses" $mOwner @{ expenseCategoryId = $firstCat.id; description = ("Smoke expense " + $runTag); amount = 500.00; userId = $me.data.user.id }
    if ($exp.ok) { Ok "expense created" } else { Bad "expense created" $exp.error }
    $expList = Call "GET" "/expenses?size=10" $mOwner
    if ($expList.ok -and @($expList.data.content).Count -ge 1) { Ok "expenses listed" } else { Bad "expenses listed" $expList.error }
} else { Bad "expense test" "no categories" }

Write-Host "== N. Insurance =="
$nOwner = New-Login "admin@demo.com" "admin123"
$insurerBody = '{"name":"Smoke Insurer ' + $runTag + '","code":"INS-' + $runTag + '","insurerType":"PRIVATE","contactPerson":"Test Contact","phoneNumber":"+254700111222","email":"ins-' + $runTag + '@test.com","status":"ACTIVE","requiresPreauth":false}'
$insurer = Call "POST" "/insurance/insurers" $nOwner $insurerBody
if ($insurer.ok) { Ok "insurer created"; $insurerId = $insurer.data.id } else { Bad "insurer created" $insurer.error }
$insList = Call "GET" "/insurance/insurers?size=50" $nOwner
if ($insList.ok -and @($insList.data.content).Count -ge 1) { Ok "insurers listed" } else { Bad "insurers listed" $insList.error }
$claimList = Call "GET" "/insurance/claims?size=10" $nOwner
if ($claimList.ok) { Ok "claims list accessible" } else { Bad "claims list" $claimList.error }

Write-Host ""
Write-Host ("RESULT: {0} passed, {1} failed" -f $script:pass, $script:fail)
if ($script:failures.Count -gt 0) { Write-Host "Failures:"; $script:failures | ForEach-Object { Write-Host (" - " + $_) }; exit 1 }
