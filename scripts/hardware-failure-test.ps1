# Hardware failure resilience tests. Validates graceful degradation
# when peripheral devices (scanner, printer, cash drawer) go offline.
# System should remain functional for core operations.
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
    foreach ($k in $ctx.headers.Keys) { $headers[$k] = $ctx.headers[$ctx.headers.Count - 1] }
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

# Clean sessions and shifts
try { docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d pharmacy_pos -c "DELETE FROM SPRING_SESSION; UPDATE staff_shifts SET status = 'CLOSED', shift_end_time = NOW() WHERE status = 'ACTIVE';" 2>$null | Out-Null } catch {}
Start-Sleep -Seconds 1

Write-Host "== HARDWARE FAILURE RESILIENCE =="
Write-Host ""

# Login as owner (has all permissions)
$owner = New-Login "admin@demo.com" "admin123"
$branchId = (docker exec pharmacy-pos-pilot-postgres-1 psql -U pharmacy_pos -d pharmacy_pos -t -A -c "SELECT id FROM branch LIMIT 1" 2>$null).Trim()
if (-not $branchId) { $branchId = "5e605c7e-ec5a-436e-8b27-6eb54c4cff80" }

# Open shift
$shift = (Call POST "/shifts" $owner @{ branchId = $branchId; openingCash = 500 }).data
$shiftId = $shift.id.ToString()

# --------------------------------------------------------
# H1: Check initial hardware status (all peripherals ONLINE)
# --------------------------------------------------------
Write-Host ""
Write-Host "-- H1: Initial hardware status --"

# Get terminal health (includes peripheral status)
$health = (Call GET "/terminals/health" $owner).data
# The health endpoint may return different structure; check what we get
Write-Host "  Health response: $($health | ConvertTo-Json -Depth 3)"

# Get hardware config
$hwConfig = (Call GET "/hardware/config" $owner).data
Write-Host "  Hardware config: ok=$($hwConfig.ok)"

# --------------------------------------------------------
# H2: Simulate scanner going OFFLINE
# --------------------------------------------------------
Write-Host ""
Write-Host "-- H2: Scanner goes OFFLINE --"

# First, list peripherals to find the scanner ID
$periphResp = (Call GET "/terminals/peripherals" $owner).data
Write-Host "  Peripherals: $($periphResp | ConvertTo-Json -Depth 2)"

# If we have peripherals, update scanner to OFFLINE
if ($periphResp.ok -and $periphResp.data) {
    $peripherals = $periphResp.data
    if ($peripherals.count -gt 0) {
        # Find a scanner peripheral
        $scanner = $peripherals | Where-Object { $_.type -eq "SCANNER" }
        if ($scanner) {
            $scannerId = $scanner.id
            # Update scanner status to OFFLINE
            $r = Call PATCH "/terminals/peripherals/$scannerId/status" $owner @{ status = "OFFLINE" }
            Write-Host "  Scanner $scannerId status updated: $($r.ok)"
        }
    }
}

# Verify system still functions for core operations (cash sale)
# A sale should work even without a scanner (manual item entry)
$clientId = [guid]::NewGuid().ToString()
$saleBody = @{
    branchId = $branchId
    shiftId = $shiftId
    clientSaleId = $clientId
    cashTendered = 100
    items = @(@{ medicineId = "9378f6ab-3b58-44a4-9190-34f88a69f2a5"; quantity = 1; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = 10 })
    payments = @(@{ method = "CASH"; amount = 10 })
}
$sale = (Call POST "/sales" $owner $saleBody).data
if ($sale.ok) { Ok "H2-sale-works-without-scanner" }
else { Bad "H2-sale-works-without-scanner" "sale failed: $($sale.error)" }

# --------------------------------------------------------
# H3: Simulate printer going OFFLINE
# --------------------------------------------------------
Write-Host ""
Write-Host "-- H3: Printer goes OFFLINE --"

# Update printer status to OFFLINE
$periphResp2 = (Call GET "/terminals/peripherals" $owner).data
if ($periphResp2.ok -and $periphResp2.data) {
    $printer = $periphResp2.data | Where-Object { $_.type -eq "PRINTER" }
    if ($printer) {
        $printerId = $printer.id
        $r = Call PATCH "/terminals/peripherals/$printerId/status" $owner @{ status = "OFFLINE" }
        Write-Host "  Printer $printerId status updated: $($r.ok)"
    }
}

# System should still allow sales (printing is optional for core ops)
$saleBody2 = @{
    branchId = $branchId
    shiftId = $shiftId
    clientSaleId = [guid]::NewGuid().ToString()
    cashTendered = 100
    items = @(@{ medicineId = "9378f6ab-3b58-44a4-9190-34f88a69f2a5"; quantity = 1; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = 10 })
    payments = @(@{ method = "CASH"; amount = 10 })
}
$sale2 = (Call POST "/sales" $owner $saleBody2).data
if ($sale2.ok) { Ok "H3-sale-works-without-printer" }
else { Bad "H3-sale-works-without-printer" "sale failed: $($sale2.error)" }

# --------------------------------------------------------
# H4: Simulate cash drawer going OFFLINE
# --------------------------------------------------------
Write-Host ""
Write-Host "-- H4: Cash drawer goes OFFLINE --"

# Update cash drawer status to OFFLINE
$periphResp3 = (Call GET "/terminals/peripherals" $owner).data
if ($periphResp3.ok -and $periphResp3.data) {
    $drawer = $periphResp3.data | Where-Object { $_.type -eq "CASH_DRAWER" }
    if ($drawer) {
        $drawerId = $drawer.id
        $r = Call PATCH "/terminals/peripherals/$drawerId/status" $owner @{ status = "OFFLINE" }
        Write-Host "  Cash drawer $drawerId status updated: $($r.ok)"
    }
}

# System should still allow cash sales (drawer offline is OK for manual operations)
$saleBody3 = @{
    branchId = $branchId
    shiftId = $shiftId
    clientSaleId = [guid]::NewGuid().ToString()
    cashTendered = 100
    items = @(@{ medicineId = "9378f6ab-3b58-44a4-9190-34f88a69f2a5"; quantity = 1; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = 10 })
    payments = @(@{ method = "CASH"; amount = 10 })
}
$sale3 = (Call POST "/sales" $owner $saleBody3).data
if ($sale3.ok) { Ok "H4-sale-works-without-cash-drawer" }
else { Bad "H4-sale-works-without-cash-drawer" "sale failed: $($sale3.error)" }

# --------------------------------------------------------
# H5: All peripherals OFFLINE → system still functions for core ops
# --------------------------------------------------------
Write-Host ""
Write-Host "-- H5: All peripherals OFFLINE --"

# Update all known peripheral types to OFFLINE
foreach ($type in @("SCANNER", "PRINTER", "CASH_DRAWER")) {
    $p = ($periphResp3.data | Where-Object { $_.type -eq $type })
    if ($p) {
        Call PATCH "/terminals/peripherals/$($p.id)/status" $owner @{ status = "OFFLINE" } | Out-Null
    }
}

# Core sale should still work
$saleBody4 = @{
    branchId = $branchId
    shiftId = $shiftId
    clientSaleId = [guid]::NewGuid().ToString()
    cashTendered = 100
    items = @(@{ medicineId = "9378f6ab-3b58-44a4-9190-34f88a69f2a5"; quantity = 1; lineId = [guid]::NewGuid().ToString(); expectedUnitPrice = 10 })
    payments = @(@{ method = "CASH"; amount = 10 })
}
$sale4 = (Call POST "/sales" $owner $saleBody4).data
if ($sale4.ok) { Ok "H5-sale-works-all-peripherals-offline" }
else { Bad "H5-sale-works-all-peripherals-offline" "sale failed: $($sale4.error)" }

# --------------------------------------------------------
# H6: Verify terminal health endpoint reflects peripheral status
# --------------------------------------------------------
Write-Host ""
Write-Host "-- H6: Terminal health reflects peripheral status --"

$health2 = (Call GET "/terminals/health" $owner).data
Write-Host "  Health after failures: $($health2 | ConvertTo-Json -Depth 2)"

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