# Seeds realistic demo data through the live API (no direct DB writes).
# Reference dictionaries (categories/units/dosage forms/manufacturers) are
# read-only by design and come from Flyway migrations V9 + V16.
#
# Usage:  .\scripts\seed-demo-data.ps1 [-ApiBase "http://localhost:9090"] [-Email admin@demo.com -Password admin123]
param(
    [string]$ApiBase = "http://localhost:9090",
    [string]$Email = "admin@demo.com",
    [string]$Password = "admin123"
)

$ErrorActionPreference = "Stop"

function New-Session {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $csrf = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $session).data
    Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/login" -Method Post `
        -Body (@{ email = $Email; password = $Password } | ConvertTo-Json) `
        -ContentType "application/json" -WebSession $session -Headers @{ $csrf.headerName = $csrf.token } | Out-Null
    $fresh = (Invoke-RestMethod -Uri "$ApiBase/api/v1/auth/csrf" -WebSession $session).data
    $session.Headers["X-XSRF-TOKEN"] = $fresh.token
    return $session
}

function Post-Json($session, $path, $body, $idempotent = $false) {
    try {
        return (Invoke-RestMethod -Uri "$ApiBase/api/v1$path" -Method Post `
            -Body ($body | ConvertTo-Json -Depth 8) `
            -ContentType "application/json" -WebSession $session `
            -Headers @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }).data
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $detail = $_.ErrorDetails.Message
        if ($idempotent -and ($status -eq 409 -or $detail -match "already exists")) {
            Write-Host "  = exists: $path"
            return $null
        }
        Write-Host "  ! POST $path -> HTTP $status :: $detail"
        return $null
    }
}

# Fixed reference rows from V9/V16 migrations.
$reference = @{
    "Analgesics"     = "10000000-0000-0000-0000-000000000002"   # Pain relief
    "Antibiotics"    = "10000000-0000-0000-0000-000000000003"
    "Antimalarials"  = "10000000-0000-0000-0000-000000000007"
    "First Aid"      = "10000000-0000-0000-0000-000000000006"   # First aid
    "Tablet"         = "20000000-0000-0000-0000-000000000007"   # Strip unit
    "StripUnit"      = "20000000-0000-0000-0000-000000000007"
    "BoxUnit"        = "20000000-0000-0000-0000-000000000008"
    "BottleUnit"     = "20000000-0000-0000-0000-000000000003"
    "PieceUnit"      = "20000000-0000-0000-0000-000000000006"
    "TabletForm"     = "50000000-0000-0000-0000-000000000001"
    "CapsuleForm"    = "50000000-0000-0000-0000-000000000002"
    "SyrupForm"      = "50000000-0000-0000-0000-000000000003"
    "CreamForm"      = "50000000-0000-0000-0000-000000000005"
    "Manufacturer"   = "30000000-0000-0000-0000-000000000002"   # Beta Healthcare
}

$session = New-Session

Write-Host "== Suppliers =="
$suppliers = @(
    @{ supplierName = "Mission for Essential Drugs"; licenseNumber = "PPB/LIC/1001"; phoneNumber = "+254700111222"; email = "sales@meds.or.ke"; contactPerson = "Grace Wanjiku"; address = "Industrial Area, Nairobi"; paymentTerms = "NET30"; status = "ACTIVE" },
    @{ supplierName = "Harleys Limited"; licenseNumber = "PPB/LIC/1002"; phoneNumber = "+254700333444"; email = "orders@harleys.co.ke"; contactPerson = "Peter Otieno"; address = "Westlands, Nairobi"; paymentTerms = "NET15"; status = "ACTIVE" },
    @{ supplierName = "Cosmos Pharmaceuticals"; licenseNumber = "PPB/LIC/1003"; phoneNumber = "+254700555666"; email = "info@cosmospharma.co.ke"; contactPerson = "Asha Mwangi"; address = "Mombasa Road, Nairobi"; paymentTerms = "CASH"; status = "ACTIVE" }
)
foreach ($s in $suppliers) { Post-Json $session "/suppliers" $s $true | Out-Null }
$supplierList = (Invoke-RestMethod -Uri "$ApiBase/api/v1/suppliers?size=50" -WebSession $session).data.content

Write-Host "== Customers =="
$customers = @(
    @{ firstName = "Jane"; lastName = "Njeri"; phoneNumber = "+254711000111"; email = "jane@example.com"; address = "Kilimani" },
    @{ firstName = "David"; lastName = "Kamau"; phoneNumber = "+254711222333"; email = "david@example.com"; address = "Kasarani" },
    @{ firstName = "Faith"; lastName = "Achieng"; phoneNumber = "+254711444555"; email = "faith@example.com"; address = "Ngong Road" },
    @{ firstName = "Walk-in"; lastName = "Customer"; phoneNumber = ""; email = ""; address = "" }
)
foreach ($c in $customers) { Post-Json $session "/customers" $c $true | Out-Null }

Write-Host "== Medicines =="
$today = Get-Date
$runTag = (Get-Random -Maximum 9999).ToString("0000")
$medicines = @(
    @{ brandName = "Paracetamol 500mg Tablets"; genericName = "Paracetamol"; strength = "500mg"; form = "TabletForm"; unit = "StripUnit"; category = "Analgesics"; buying = 25.00; selling = 40.00; reorder = 20 },
    @{ brandName = "Amoxicillin 250mg Capsules"; genericName = "Amoxicillin"; strength = "250mg"; form = "CapsuleForm"; unit = "StripUnit"; category = "Antibiotics"; buying = 60.00; selling = 95.00; reorder = 15 },
    @{ brandName = "Coartem 20/120mg"; genericName = "Artemether/Lumefantrine"; strength = "20/120mg"; form = "TabletForm"; unit = "BoxUnit"; category = "Antimalarials"; buying = 180.00; selling = 280.00; reorder = 10 },
    @{ brandName = "Ibuprofen 400mg Tablets"; genericName = "Ibuprofen"; strength = "400mg"; form = "TabletForm"; unit = "StripUnit"; category = "Analgesics"; buying = 35.00; selling = 60.00; reorder = 20 },
    @{ brandName = "Cetirizine 10mg Tablets"; genericName = "Cetirizine Hydrochloride"; strength = "10mg"; form = "TabletForm"; unit = "StripUnit"; category = "Analgesics"; buying = 20.00; selling = 45.00; reorder = 15 },
    @{ brandName = "Betadine 10% Solution"; genericName = "Povidone Iodine"; strength = "10%"; form = "CreamForm"; unit = "BottleUnit"; category = "First Aid"; buying = 140.00; selling = 220.00; reorder = 8 },
    @{ brandName = "ORS Sachets"; genericName = "Oral Rehydration Salts"; strength = "20.5g"; form = "SyrupForm"; unit = "PieceUnit"; category = "First Aid"; buying = 18.00; selling = 35.00; reorder = 25 }
)
$allMeds = (Invoke-RestMethod -Uri "$ApiBase/api/v1/medicines?size=200&sort=brandName,asc" -WebSession $session).data.content
foreach ($m in $medicines) {
    if ($allMeds | Where-Object brandName -eq $m.brandName) { Write-Host "  = medicine exists: $($m.brandName)"; continue }
    $payload = @{
        brandName = $m.brandName; genericName = $m.genericName; strength = $m.strength
        buyingPrice = $m.buying; sellingPrice = $m.selling; reorderLevel = $m.reorder
        manufacturerId = $reference["Manufacturer"]
        medicineCategoriesId = $reference[$m.category]
        dosageFormId = $reference[$m.form]
        unitId = $reference[$m.unit]
        status = "AVAILABLE"
        trackSerialNumber = $false; trackBatch = $true; trackExpiry = $true
        requiresPrescription = ($m.category -eq "Antibiotics"); requiresRefrigeration = $false; isControlledDrug = $false
    }
    $created = Post-Json $session "/medicines" $payload $true
    if ($null -ne $created) { Write-Host "  + medicine: $($m.brandName)" }
}
$allMeds = (Invoke-RestMethod -Uri "$ApiBase/api/v1/medicines?size=200&sort=brandName,asc" -WebSession $session).data.content

Write-Host "== Goods received notes (stock) =="
$existingStock = (Invoke-RestMethod -Uri "$ApiBase/api/v1/stock?size=1000" -WebSession $session).data.content
$grnCount = 0
foreach ($m in $allMeds) {
    $hasStock = @($existingStock | Where-Object { $_.medicineId -eq $m.id -and $_.quantityAvailable -gt 0 })
    if ($hasStock.Count -ge 2) { continue }

    $supplier = $supplierList[(Get-Random -Minimum 0 -Maximum $supplierList.Count)]
    $prefix = ($m.brandName -replace "[^a-zA-Z]", "").Substring(0, 6) + "-" + $runTag
    # Batch 1: healthy stock, long-dated.
    $grnBody = @{
        supplierId = $supplier.id
        supplierInvoiceNumber = "INV-" + (Get-Date -Format "yyMMdd") + "-" + (Get-Random -Maximum 999)
        purchaseOrdersId = $null
        receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        remarks = "Initial demo stock"
        lines = @(@{
            medicineId = $m.id
            batchNumber = "${prefix}-B1"
            expiryDate = $today.AddMonths(14).ToString("yyyy-MM-dd")
            quantity = 80
            unitCost = $m.buyingPrice
            purchaseOrderLineId = $null
        })
    }
    $r = Post-Json $session "/goods-received" $grnBody
    if ($null -ne $r) { $grnCount++; Write-Host "  + GRN healthy batch: $($m.brandName)" }

    # Batch 2: small quantity inside expiry window (drives LOW_STOCK + EXPIRY alerts).
    $grnBody.supplierInvoiceNumber = "INV-" + (Get-Date -Format "yyMMdd") + "-" + (Get-Random -Maximum 999)
    $grnBody.lines = @(@{
        medicineId = $m.id
        batchNumber = "${prefix}-B2"
        expiryDate = $today.AddDays(45).ToString("yyyy-MM-dd")
        quantity = 5
        unitCost = $m.buyingPrice
        purchaseOrderLineId = $null
    })
    $r2 = Post-Json $session "/goods-received" $grnBody
    if ($null -ne $r2) { $grnCount++; Write-Host "  + GRN near-expiry batch: $($m.brandName)" }
}
Write-Host "  GRNs created this run: $grnCount"

$finalStock = (Invoke-RestMethod -Uri "$ApiBase/api/v1/stock?size=1000" -WebSession $session).data.content
$finalCustomers = (Invoke-RestMethod -Uri "$ApiBase/api/v1/customers?size=100" -WebSession $session).data.content
Write-Host ""
Write-Host "Seed summary:"
Write-Host ("  medicines : {0}" -f $allMeds.Count)
Write-Host ("  suppliers : {0}" -f $supplierList.Count)
Write-Host ("  customers : {0}" -f $finalCustomers.Count)
Write-Host ("  stock rows: {0}" -f $finalStock.Count)
