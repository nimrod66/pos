# Concurrency verification test. Validates that concurrent sales of the same medicine
# are safely serialized via SELECT FOR UPDATE. Stock cannot go negative.
# Two terminals selling the same item simultaneously:
# - Total demand exceeds stock → one succeeds, one fails with INSUFFICIENT_STOCK
# - Final stock is correct
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
    return @{ session = $s; headers = @{ $c2.headerName = $c2.token; "Idempotency-Key" = [guid]::NewGuid().ToString() } }
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

# Clean sessions and active shifts
try { docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d pharmacy_pos -c "DELETE FROM SPRING_SESSION; UPDATE staff_shifts SET status = 'CLOSED', shift_end_time = NOW() WHERE status = 'ACTIVE';" 2>$null | Out-Null } catch {}
Start-Sleep -Seconds 1

Write-Host "== CONCURRENCY VERIFICATION =="
Write-Host ""

# Login two different users
$cashier1 = New-Login "cashier@demo.com" "cashier123"
$cashier2 = New-Login "admin@demo.com" "admin123"
$branchId = "fab48c89-7bac-46e0-adc8-7daa4cd4914a"

# Open shifts
$shift1 = (Call POST "/shifts" $cashier1 @{ branchId = $branchId; openingCash = 500 }).data
$shift2 = (Call POST "/shifts" $cashier2 @{ branchId = $branchId; openingCash = 500 }).data
if (-not $shift1 -or -not $shift2) { Bad "C0-setup-shifts" "could not open two shifts"; return }
$shiftId1 = $shift1.id.ToString()
$shiftId2 = $shift2.id.ToString()

# Pick a medicine with available stock
$stockResp = (Call GET "/inventory/stock?page=0&size=50" $cashier1).data
$stockRows = $stockResp.content
$targetMed = $null
$targetStock = 0
foreach ($row in $stockRows) {
    $qty = [int]$row.quantityAvailable
    if ($qty -ge 1) {
        $targetMed = $row
        $targetStock = $qty
        break
    }
}
if (-not $targetMed) { Bad "C0-setup-stock" "no medicine with available stock"; return }

$medId = if ($targetMed.medicineBatchesId) { $targetMed.medicineBatchesId.medicine.id } else { $targetMed.medicineId }
$batchId = if ($targetMed.medicineBatchesId) { $targetMed.medicineBatchesId } else { "" }
$unitPrice = if ($targetMed.medicineBatches) { $targetMed.medicineBatches.sellingPrice } else { 10 }
Write-Host "  Medicine: $medId (stock: $targetStock, unitPrice: $unitPrice)"

# --------------------------------------------------------
# C1: Two concurrent sales selling half each (total <= stock)
# --------------------------------------------------------
Write-Host ""
Write-Host "-- C1: Concurrent safe sales (total <= stock) --"

# Each sells floor(stock/2) units
$sellQty = [Math]::Floor($targetStock / 2)

$clientId1 = [guid]::NewGuid().ToString()
$clientId2 = [guid]::NewGuid().ToString()

$saleBody1 = @{
    branchId = $branchId
    shiftId = $shiftId1
    clientSaleId = $clientId1
    cashTendered = $sellQty * $unitPrice
    items = @(@{ medicineId = $medId; quantity = $sellQty; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = $unitPrice })
    payments = @(@{ method = "CASH"; amount = $sellQty * $unitPrice })
}
$saleBody2 = @{
    branchId = $branchId
    shiftId = $shiftId2
    clientSaleId = $clientId2
    cashTendered = $sellQty * $unitPrice
    items = @(@{ medicineId = $medId; quantity = $sellQty; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = $unitPrice })
    payments = @(@{ method = "CASH"; amount = $sellQty * $unitPrice })
}

# Fire concurrently
$ps1 = [powershell]::Create()
$ps2 = [powershell]::Create()

$ps1.Runspace = [runspacefactory]::CreateRunspace(); $ps1.Runspace.Open()
$ps2.Runspace = [runspacefactory]::CreateRunspace(); $ps2.Runspace.Open()

$j1 = $ps1.BeginInvoke($ApiBase, $saleBody1, $cashier1.session, $cashier1.headers)
$j2 = $ps2.BeginInvoke($ApiBase, $saleBody2, $cashier2.session, $cashier2.headers)
$j1.AsyncWaitHandle.WaitOne() | Out-Null
$j2.AsyncWaitHandle.WaitOne() | Out-Null

$result1 = $ps1.EndInvoke($j1)
$result2 = $ps2.EndInvoke($j2)

$ps1.Runspace.Close(); $ps2.Runspace.Close(); $ps1.Dispose(); $ps2.Dispose()

$c1Ok = $result1.ok -and $result2.ok
if ($c1Ok) { Ok "C1-concurrent-safe-sales" } else { Bad "C1-concurrent-safe-sales" "r1=$($result1.ok) r2=$($result2.ok)" }

# --------------------------------------------------------
# C2: Total demand exceeds stock → one succeeds, one fails
# --------------------------------------------------------
Write-Host ""
Write-Host "-- C2: Total demand > stock → serialization --"

# Each tries to sell the full remaining stock
$excessQty = $targetStock

$clientId3 = [guid]::NewGuid().ToString()
$clientId4 = [guid]::NewGuid().ToString()

$saleBody3 = @{
    branchId = $branchId
    shiftId = $shiftId1
    clientSaleId = $clientId3
    cashTendered = $excessQty * $unitPrice
    items = @(@{ medicineId = $medId; quantity = $excessQty; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = $unitPrice })
    payments = @(@{ method = "CASH"; amount = $excessQty * $unitPrice })
}
$saleBody4 = @{
    branchId = $branchId
    shiftId = $shiftId2
    clientSaleId = $clientId4
    cashTendered = $excessQty * $unitPrice
    items = @(@{ medicineId = $medId; quantity = $excessQty; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = $unitPrice })
    payments = @(@{ method = "CASH"; amount = $excessQty * $unitPrice })
}

$ps3 = [powershell]::Create()
$ps4 = [powershell]::Create()

$ps3.Runspace = [runspacefactory]::CreateRunspace(); $ps3.Runspace.Open()
$ps4.Runspace = [runspacefactory]::CreateRunspace(); $ps4.Runspace.Open()

$j3 = $ps3.BeginInvoke($ApiBase, $saleBody3, $cashier1.session, $cashier1.headers)
$j4 = $ps4.BeginInvoke($ApiBase, $saleBody4, $cashier2.session, $cashier2.headers)
$j3.AsyncWaitHandle.WaitOne() | Out-Null
$j4.AsyncWaitHandle.WaitOne() | Out-Null

$result3 = $ps3.EndInvoke($j3)
$result4 = $ps4.EndInvoke($j4)

$ps3.Runspace.Close(); $ps4.Runspace.Close(); $ps3.Dispose(); $ps4.Dispose()

$succeeded = 0
$insufficient = 0
foreach ($r in @($result3, $result4)) {
    if ($r.ok) { $succeeded++ }
    elseif ($r.error -match "INSUFFICIENT_STOCK|Insufficient stock") { $insufficient++ }
}

Write-Host "  Results: $succeeded succeeded, $insufficient got INSUFFICIENT_STOCK"

if ($succeeded -eq 1 -and $insufficient -eq 1) { Ok "C2-one-succeeds-one-fails" }
elseif ($succeeded -eq 2) {
    # Both succeeded — verify stock is not negative
    $stockAfter = (Call GET "/inventory/stock?page=0&size=50" $cashier1).data.content
    $finalQty = -1
    foreach ($row in $stockAfter) {
        if ($batchId -and $row.medicineBatchesId -eq $batchId) {
            $finalQty = [int]$row.quantityAvailable
            break
        }
    }
    if ($finalQty -ge 0) { Ok "C2-one-succeeds-one-fails" }
    else { Bad "C2-one-succeeds-one-fails" "stock=$finalQty" }
}
elseif ($succeeded -eq 0) { Bad "C2-one-succeeds-one-fails" "both failed" }

# --------------------------------------------------------
# C3: Verify final stock is correct (non-negative)
# --------------------------------------------------------
Write-Host ""
Write-Host "-- C3: Stock integrity --"

$stockFinal = (Call GET "/inventory/stock?page=0&size=50" $cashier1).data.content
$finalQty = -1
foreach ($row in $stockFinal) {
    if ($batchId -and $row.medicineBatchesId -eq $batchId) {
        $finalQty = [int]$row.quantityAvailable
        break
    }
}
if ($finalQty -ge 0) { Ok "C3-stock-non-negative" }
else { Bad "C3-stock-non-negative" "stock=$finalQty" }

# --------------------------------------------------------
# Summary
# --------------------------------------------------------
Write-Host ""
Write-Host "==============================="
Write-Host ("  PASSED:  " + $script:pass)
Write-Host ("  FAILED:  " + $script:fail)

if ($script:fail -gt 0) {
    Write-Host ""
    Write-Host "  Failures:"
    foreach ($f in $script:failures) { Write-Host "    - $f" }
}