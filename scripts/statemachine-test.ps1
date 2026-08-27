# State machine verification tests. Validates that every status
# transition is enforced and invalid transitions are blocked.
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
    Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
        -Body $loginBody `
        -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName = $c.token } | Out-Null
    $c2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    return @{ session = $s; headers = @{ $c2.headerName = $c2.token } }
}

function Call($method, $path, $ctx, $body) {
    $headers = @{}
    foreach ($k in $ctx.headers.Keys) { $headers[$k] = $ctx.headers[$k] }
    if (-not $headers.ContainsKey("Idempotency-Key")) {
        $headers["Idempotency-Key"] = [guid]::NewGuid().ToString()
    }
    $params = @{ Uri = "$ApiBase/api/v1$path"; Method = $method; WebSession = $ctx.session; Headers = $headers }
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

function Call-Patch($path, $ctx, $body) {
    return Call "PATCH" $path $ctx $body
}

# Clean stale sessions and active shifts
try { docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d pharmacy_pos -c "DELETE FROM SPRING_SESSION; UPDATE staff_shifts SET status = 'CLOSED' WHERE status = 'ACTIVE';" 2>$null | Out-Null } catch {}
Start-Sleep -Seconds 1

Write-Host "== STATE MACHINE VERIFICATION =="
Write-Host ""

# Helper: login and get shift ID using direct Invoke-RestMethod
function Login-And-GetShift($email, $passw) {
    $s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $c = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    $loginBody = @{ email = $email; password = $passw } | ConvertTo-Json
    Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
        -Body $loginBody `
        -ContentType "application/json" -WebSession $s -Headers @{ $c.headerName = $c.token } | Out-Null
    $c2 = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $s).data
    $ctx = @{ session = $s; headers = @{ $c2.headerName = $c2.token; "Idempotency-Key" = [guid]::NewGuid().ToString() } }
    return $ctx
}

# ============================================================
# S1: SALE STATE MACHINE
# ============================================================
Write-Host "-- S1: Sale state machine --"
$owner = New-Login "admin@demo.com" "admin123"

# Get a medicine and branch for testing
$medsResp = (Call GET "/medicines?page=0&size=1" $owner).data
$medData = $medsResp.content[0]
$medId = $medData.id
$medPrice = if ($medData.sellingPrice) { $medData.sellingPrice } else { 10 }
$branchId = "fab48c89-7bac-46e0-adc8-7daa4cd4914a"

# Open a shift first (required for sales)
$shiftBody = @{ branchId = $branchId; openingCash = 1000 }
$shift = (Call POST "/shifts" $owner $shiftBody).data
if (-not $shift -or -not $shift.id) { Bad "S1-setup-shift" "could not open shift"; return }
$shiftId = $shift.id.ToString()

# S1a: Create a cash sale → should complete (COMPLETED)
$saleClientId = [guid]::NewGuid().ToString()
$saleBody = @{
    branchId = $branchId
    shiftId = $shiftId
    clientSaleId = $saleClientId
    cashTendered = $medPrice
    items = @(@{ medicineId = $medId; quantity = 1; unitPrice = $medPrice; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = $medPrice })
    payments = @(@{ method = "CASH"; amount = $medPrice })
}
# Use the same idempotency key as clientSaleId
$saleHeaders = @{ "Idempotency-Key" = $saleClientId }
$saleCtx = @{ session = $owner.session; headers = $saleHeaders }
$saleResp = Call POST "/sales" $saleCtx $saleBody
if ($saleResp.ok -and $saleResp.data -and $saleResp.data.id) {
    # Verify via GET (POST response may not populate status in all cases)
    $saleCheck = Call GET "/sales/$($saleResp.data.id)" $owner
    $saleStatus = if ($saleCheck.ok) { $saleCheck.data.status } else { $saleResp.data.status }
    if ($saleStatus -eq "COMPLETED" -or $saleStatus -eq "DONE") { Ok "S1a-cash-sale-completes" } else { Bad "S1a-cash-sale-completes" "status=$saleStatus" }
} else { Bad "S1a-cash-sale-completes" "sale not created: $($saleResp.error)" }

# S1b: Try to cancel a COMPLETED sale → should fail
$r = Call POST "/sales/$($sale.id)/cancel" $owner
if (-not $r.ok) { Ok "S1b-cancel-completed-sale-blocked" } else { Bad "S1b-cancel-completed-sale-blocked" "should fail but got 200" }

# S1c: Try to suspend a COMPLETED sale → should fail
$r = Call POST "/sales/$($sale.id)/suspend" $owner
if (-not $r.ok) { Ok "S1c-suspend-completed-sale-blocked" } else { Bad "S1c-suspend-completed-sale-blocked" "should fail but got 200" }

# S1d: Try to resume a COMPLETED sale → should fail
$r = Call POST "/sales/$($sale.id)/resume" $owner
if (-not $r.ok) { Ok "S1d-resume-completed-sale-blocked" } else { Bad "S1d-resume-completed-sale-blocked" "should fail but got 200" }

# ============================================================
# S2: SHIFT STATE MACHINE
# ============================================================
Write-Host ""
Write-Host "-- S2: Shift state machine --"
$cashier = New-Login "cashier@demo.com" "cashier123"

# S2a: Open a shift → should be ACTIVE
$shiftBody = @{ branchId = $branchId; openingCash = 1000 }
$shift = (Call POST "/shifts" $cashier $shiftBody).data
if ($shift.status -eq "ACTIVE") { Ok "S2a-shift-opens" } else { Bad "S2a-shift-opens" "status=$($shift.status)" }

# S2b: Try to open another shift while one is active → should fail
$r = Call POST "/shifts" $cashier $shiftBody
if (-not $r.ok) { Ok "S2b-duplicate-shift-blocked" } else { Bad "S2b-duplicate-shift-blocked" "should fail" }

# S2c: Close the shift → should be CLOSED
$closeBody = @{ actualCash = 1000 }
$r = Call PATCH "/shifts/$($shift.id)/close" $cashier $closeBody
if ($r.ok -and ($r.data.status -eq "CLOSED")) { Ok "S2d-shift-closes" } else { Bad "S2d-shift-closes" "status=$($r.data.status)" }

# S2d: Try to close an already closed shift → should fail
$r = Call PATCH "/shifts/$($shift.id)/close" $cashier $closeBody
if (-not $r.ok) { Ok "S2e-close-closed-shift-blocked" } else { Bad "S2e-close-closed-shift-blocked" "should fail" }

# ============================================================
# S3: STOCK TRANSFER STATE MACHINE
# ============================================================
Write-Host ""
Write-Host "-- S3: Stock transfer state machine --"
$storekeeper = New-Login "storekeeper@demo.com" "stock1234"

# Find two different branches
$branches = (Call GET "/branches?pharmacyId=fab48c89-7bac-46e0-adc8-7daa4cd4914a" $storekeeper).data
if (-not $branches) { $branches = @(@{ id = "fab48c89-7bac-46e0-adc8-7daa4cd4914a"; branchName = "Main" }; @{ id = "21b203ef-34e9-4a00-986d-ebf8e3a117e3"; branchName = "Smoke" }) }
$srcBranch = $branches[0].id
$dstBranch = if ($branches.Count -gt 1) { $branches[1].id } else { $branches[0].id }

    # Get a medicine batch to transfer
    $stock = (Call GET "/stock?page=0&size=10" $storekeeper).data
    if ($stock -and $stock.content -and $stock.content.Count -gt 0) {
        # Find a batch with sufficient stock on the source branch
        $batch = $null
        foreach ($item in $stock.content) {
            if ($item.branchId -eq $srcBranch -and $item.quantityAvailable -ge 5) {
                $batch = $item
                break
            }
        }
        if (-not $batch -and $stock.content.Count -gt 0) {
            # Use any available batch
            $batch = $stock.content[0]
            $srcBranch = $batch.branchId
        }
    }
    if ($batch) {
            $batchId = $batch.medicineBatchesId
    $available = $batch.quantityAvailable

    if ($srcBranch -ne $dstBranch -and $available -ge 5) {
        # S3a: Create transfer → PENDING
        $xferBody = @{
            sourceBranchId = $srcBranch
            destBranchId = $dstBranch
            items = @(@{ medicineBatchesId = $batchId; quantity = 2 })
        }
        $xferResp = Call POST "/stock-transfers" $storekeeper $xferBody
        if ($xferResp.ok -and $xferResp.data) {
            $xferStatus = $xferResp.data.status
            if (-not $xferStatus) {
                # Check via GET
                $xferCheck = Call GET "/stock-transfers/$($xferResp.data.id)" $storekeeper
                $xferStatus = if ($xferCheck.ok) { $xferCheck.data.status } else { "UNKNOWN" }
            }
            if ($xferStatus -eq "PENDING") { Ok "S3a-transfer-creates-pending" } else { Bad "S3a-transfer-creates-pending" "status=$xferStatus" }

            # S3b: Approve → IN_TRANSIT
            $r = Call PATCH "/stock-transfers/$($xferResp.data.id)/approve" $storekeeper
            if ($r.ok -and $r.data) {
                $approveStatus = $r.data.status
                if (-not $approveStatus) {
                    $xferCheck2 = Call GET "/stock-transfers/$($xferResp.data.id)" $storekeeper
                    $approveStatus = if ($xferCheck2.ok) { $xferCheck2.data.status } else { "UNKNOWN" }
                }
                if ($approveStatus -eq "IN_TRANSIT") { Ok "S3b-transfer-approves" } else { Bad "S3b-transfer-approves" "status=$approveStatus" }
            } else { Bad "S3b-transfer-approves" "err=$($r.error)" }

            # S3c: Try to approve again → should fail
            $r = Call PATCH "/stock-transfers/$($xferResp.data.id)/approve" $storekeeper
            if (-not $r.ok) { Ok "S3c-approve-intransit-blocked" } else { Bad "S3c-approve-intransit-blocked" "should fail" }

            # S3d: Receive → RECEIVED
            $r = Call PATCH "/stock-transfers/$($xferResp.data.id)/receive" $storekeeper
            if ($r.ok -and $r.data) {
                $recvStatus = $r.data.status
                if (-not $recvStatus) {
                    $xferCheck3 = Call GET "/stock-transfers/$($xferResp.data.id)" $storekeeper
                    $recvStatus = if ($xferCheck3.ok) { $xferCheck3.data.status } else { "UNKNOWN" }
                }
                if ($recvStatus -eq "RECEIVED") { Ok "S3d-transfer-receives" } else { Bad "S3d-transfer-receives" "status=$recvStatus" }
            } else { Bad "S3d-transfer-receives" "err=$($r.error)" }

            # S3e: Try to receive again → should fail
            $r = Call PATCH "/stock-transfers/$($xferResp.data.id)/receive" $storekeeper
            if (-not $r.ok) { Ok "S3e-receive-received-blocked" } else { Bad "S3e-receive-received-blocked" "should fail" }
        } else { Bad "S3a-transfer-creates-pending" "err=$($xferResp.error)" }
    } else {
        Write-Host "  SKIP S3 (same branch or insufficient stock)"
    }
} else {
    Write-Host "  SKIP S3 (no stock found)"
}

# ============================================================
# S4: PURCHASE ORDER STATE MACHINE
# ============================================================
Write-Host ""
Write-Host "-- S4: Purchase order state machine --"
$storekeeper = New-Login "storekeeper@demo.com" "stock1234"

# Get a supplier
$suppliersResp = (Call GET "/suppliers?page=0&size=5" $storekeeper).data
$suppliers = if ($suppliersResp.content) { $suppliersResp.content } else { @() }
if ($suppliers.Count -gt 0) {
    $supplierId = $suppliers[0].id
    $medsListResp = (Call GET "/medicines?page=0&size=1" $storekeeper).data
    $testMedId = $medsListResp.content[0].id

    # S4a: Create PO → ORDERED
    $meResp = Call GET "/auth/me" $storekeeper
    $meId = ""
    if ($meResp.ok -and $meResp.data) {
        $meId = $meResp.data.id
        if (-not $meId) { $meId = $meResp.data.userId }
        if (-not $meId) {
            # Try nested user object
            if ($meResp.data.user) { $meId = $meResp.data.user.id }
        }
    }
    if (-not $meId) {
        # Fallback: query users endpoint
        $usersResp = Call GET "/users?page=0&size=1" $storekeeper
        if ($usersResp.ok -and $usersResp.data.content) { $meId = $usersResp.data.content[0].id }
    }
    $poBody = @{
        supplierId = $supplierId
        branchId = $srcBranch
        orderedById = $meId
        items = @(@{ medicineId = $testMedId; quantity = 10; buyingPrice = 50 })
    }
    $poResp = Call POST "/purchase-orders" $storekeeper $poBody
    if ($poResp.ok -and $poResp.data) {
        $po = $poResp.data
        $poStatus = $po.status
        if (-not $poStatus) {
            $poCheck = Call GET "/purchase-orders/$($po.id)" $storekeeper
            $poStatus = if ($poCheck.ok) { $poCheck.data.status } else { "UNKNOWN" }
        }
        if ($poStatus -eq "ORDERED") { Ok "S4a-po-creates-ordered" } else { Bad "S4a-po-creates-ordered" "status=$poStatus" }

    # S4b: Approve → IN_PROGRESS (use owner for approve permission)
    $meResp = Call GET "/auth/me" $owner
    $meId = ""
    if ($meResp.ok -and $meResp.data) {
        if ($meResp.data.user) { $meId = $meResp.data.user.id }
        elseif ($meResp.data.id) { $meId = $meResp.data.id }
    }
    $r = Call PATCH "/purchase-orders/$($po.id)/approve?userId=$meId" $owner
        if ($r.ok -and $r.data) {
            $approveStatus = $r.data.status
            if (-not $approveStatus) {
                $poCheck2 = Call GET "/purchase-orders/$($po.id)" $storekeeper
                $approveStatus = if ($poCheck2.ok) { $poCheck2.data.status } else { "UNKNOWN" }
            }
            if ($approveStatus -eq "IN_PROGRESS") { Ok "S4b-po-approves" } else { Bad "S4b-po-approves" "status=$approveStatus" }
        } else { Bad "S4b-po-approves" "err=$($r.error)" }

        # S4c: Try to approve again → should fail
        $r = Call PATCH "/purchase-orders/$($po.id)/approve?userId=$meId" $owner
        if (-not $r.ok) { Ok "S4c-approve-inprogress-blocked" } else { Bad "S4c-approve-inprogress-blocked" "should fail" }

        # S4d: Receive via GRN (partial) → stays IN_PROGRESS
        $poLineId = $po.items[0].id
        $expiryDate = (Get-Date).AddYears(1).ToString("yyyy-MM-dd")
        $grnBody = @{
            supplierId = $supplierId
            purchaseOrdersId = $po.id
            lines = @(@{ medicineId = $testMedId; purchaseOrderLineId = $poLineId; batchNumber = "GRN-TEST-001"; expiryDate = $expiryDate; quantity = 5; unitCost = 50 })
        }
        $grn = Call POST "/goods-received" $storekeeper $grnBody
        # Check PO status after partial GRN
        $poAfter = Call GET "/purchase-orders/$($po.id)" $storekeeper
        $poAfterStatus = if ($poAfter.ok) { $poAfter.data.status } else { "UNKNOWN" }
        if ($poAfterStatus -eq "IN_PROGRESS") { Ok "S4d-partial-grn-stays-inprogress" } else { Bad "S4d-partial-grn-stays-inprogress" "status=$poAfterStatus" }

        # S4e: Receive remaining → DELIVERED
        $grnBody2 = @{
            supplierId = $supplierId
            purchaseOrdersId = $po.id
            lines = @(@{ medicineId = $testMedId; purchaseOrderLineId = $poLineId; batchNumber = "GRN-TEST-002"; expiryDate = $expiryDate; quantity = 5; unitCost = 50 })
        }
        $grn2 = Call POST "/goods-received" $storekeeper $grnBody2
        $poFinal = Call GET "/purchase-orders/$($po.id)" $storekeeper
        $poFinalStatus = if ($poFinal.ok) { $poFinal.data.status } else { "UNKNOWN" }
        if ($poFinalStatus -eq "DELIVERED") { Ok "S4e-full-grn-delivers" } else { Bad "S4e-full-grn-delivers" "status=$poFinalStatus" }

        # S4f: Try to receive on a DELIVERED PO → should fail
        $grnBody3 = @{
            supplierId = $supplierId
            purchaseOrdersId = $po.id
            lines = @(@{ medicineId = $testMedId; purchaseOrderLineId = $poLineId; batchNumber = "GRN-TEST-003"; expiryDate = $expiryDate; quantity = 1; unitCost = 50 })
        }
        $r = Call POST "/goods-received" $storekeeper $grnBody3
        if (-not $r.ok) { Ok "S4f-receive-delivered-blocked" } else { Bad "S4f-receive-delivered-blocked" "should fail" }
    } else { Bad "S4a-po-creates-ordered" "err=$($poResp.error)" }
} else {
    Write-Host "  SKIP S4 (no suppliers)"
}

# ============================================================
# S5: PRESCRIPTION STATE MACHINE
# ============================================================
Write-Host ""
Write-Host "-- S5: Prescription state machine --"
$pharmacist = New-Login "pharmacist@demo.com" "pharmacist123"

$medsList = (Call GET "/medicines?page=0&size=1" $pharmacist).data
$testMedId = $medsList.content[0].id

# S5a: Create prescription → ACTIVE
$rxBody = @{
    branchId = $branchId
    customerName = "State Machine Test"
    doctorName = "Dr. Test"
    doctorLicenseNumber = "TEST-001"
    prescriptionNumber = "RX-SM-" + (Get-Random -Maximum 99999)
    issuedDate = (Get-Date).ToString("yyyy-MM-dd")
    items = @(@{ medicineId = $testMedId; quantity = 2; dosage = "500mg" })
}
$rx = (Call POST "/prescriptions" $pharmacist $rxBody).data
if ($rx.status -eq "ACTIVE") { Ok "S5a-prescription-creates-active" } else { Bad "S5a-prescription-creates-active" "status=$($rx.status)" }

# S5b: Dispense → DISPENSED
$dispResp = Call PATCH "/prescriptions/$($rx.id)/dispense" $pharmacist
if ($dispResp.ok) {
    $rxCheck = Call GET "/prescriptions/$($rx.id)" $pharmacist
    $rxStatus = if ($rxCheck.ok) { $rxCheck.data.status } else { $dispResp.data.status }
    if ($rxStatus -eq "DISPENSED") { Ok "S5b-prescription-dispenses" } else { Bad "S5b-prescription-dispenses" "status=$rxStatus" }
} else { Bad "S5b-prescription-dispenses" "dispense failed: $($dispResp.error)" }

# S5c: Try to dispense again → should fail
$r = Call PATCH "/prescriptions/$($rx.id)/dispense" $pharmacist
if (-not $r.ok) { Ok "S5c-redispense-blocked" } else { Bad "S5c-redispense-blocked" "should fail" }

# S5d: Try to use a DISPENSED prescription in a sale → should fail
$saleBody = @{
    branchId = $branchId
    prescriptionId = $rx.id
    clientSaleId = [guid]::NewGuid().ToString()
    items = @(@{ medicineId = $testMedId; quantity = 1; unitPrice = 10; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = 10 })
    payments = @(@{ method = "CASH"; amount = 10 })
}
$r = Call POST "/sales" $pharmacist $saleBody
if (-not $r.ok) { Ok "S5d-dispensed-rx-in-sale-blocked" } else { Bad "S5d-dispensed-rx-in-sale-blocked" "should fail" }

# ============================================================
# SUMMARY
# ============================================================
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
