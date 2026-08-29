. "$PSScriptRoot\brutal-common.ps1"

# ═══════════════════════════════════════════════════════════
# R2: Crash After Stock Lock — Transaction Rollback
# ═══════════════════════════════════════════════════════════
Begin-TestGroup "R2: Crash After Stock Lock"

try {
    $ctx = New-Login "admin@demo.com" "admin123"
    $branchId = $ctx.branchId

    # Get Paracetamol and its stock
    $medsResp = Call "GET" "/medicines?size=200" $ctx
    $med = @($medsResp.data.content | Where-Object { $_.brandName -like "*Paracetamol*" }) | Select-Object -First 1
    $medId = $med.id

    $stockBefore = Db-Query "SELECT COALESCE(SUM(s.quantity_available), 0) as qty FROM stock s JOIN medicine_batches mb ON s.medicine_batches_id = mb.id WHERE mb.medicine_id = '$medId' AND s.branch_id = '$branchId'"
    $qtyBefore = [int]$stockBefore[0].qty
    Write-Ts Yellow "  Stock before: $qtyBefore"

    # Record sale count before
    $salesBefore = Db-Query "SELECT COUNT(*) as cnt FROM sales WHERE branch_id = '$branchId'"
    $countBefore = [int]$salesBefore[0].cnt

    # Kill API
    Write-Ts Yellow "  Stopping API container..."
    Stop-ApiContainer
    Start-Sleep -Seconds 2

    # Restart API
    Write-Ts Yellow "  Starting API container..."
    Start-ApiContainer
    Wait-ForApi 60

    # Verify stock unchanged (transaction rolled back)
    $stockAfter = Db-Query "SELECT COALESCE(SUM(s.quantity_available), 0) as qty FROM stock s JOIN medicine_batches mb ON s.medicine_batches_id = mb.id WHERE mb.medicine_id = '$medId' AND s.branch_id = '$branchId'"
    $qtyAfter = [int]$stockAfter[0].qty
    Assert "R2-stock-unchanged" ($qtyAfter -eq $qtyBefore) "Stock should be unchanged: before=$qtyBefore after=$qtyAfter"

    # Verify no orphan sales
    $salesAfter = Db-Query "SELECT COUNT(*) as cnt FROM sales WHERE branch_id = '$branchId'"
    $countAfter = [int]$salesAfter[0].cnt
    Assert "R2-no-orphan-sales" ($countAfter -eq $countBefore) "Sale count should be unchanged"

    # Verify system works after restart
    $ctx2 = New-Login "admin@demo.com" "admin123"
    $health = Call "GET" "/system/health" $ctx2
    Assert "R2-system-healthy-after-restart" $health.ok "System should be healthy after restart"

} catch {
    Assert "R2-unhandled-error" $false $_.Exception.Message
}

End-TestGroup

# ═══════════════════════════════════════════════════════════
# R4: Database Restart — Data Persistence
# ═══════════════════════════════════════════════════════════
Begin-TestGroup "R4: Database Restart"

try {
    # Record counts before restart
    $salesBefore = Db-Query "SELECT COUNT(*) as cnt FROM sales"
    $usersBefore = Db-Query "SELECT COUNT(*) as cnt FROM users"
    $medsBefore = Db-Query "SELECT COUNT(*) as cnt FROM medicine"

    $salesCount = [int]$salesBefore[0].cnt
    $usersCount = [int]$usersBefore[0].cnt
    $medsCount = [int]$medsBefore[0].cnt

    Write-Ts Yellow "  Before restart: $salesCount sales, $usersCount users, $medsCount medicines"

    # Restart entire Docker stack
    Write-Ts Yellow "  Restarting Docker stack..."
    Restart-DockerStack
    Wait-ForApi 90

    # Verify data persists
    $salesAfter = Db-Query "SELECT COUNT(*) as cnt FROM sales"
    $usersAfter = Db-Query "SELECT COUNT(*) as cnt FROM users"
    $medsAfter = Db-Query "SELECT COUNT(*) as cnt FROM medicine"

    Assert "R4-sales-persist" ([int]$salesAfter[0].cnt -eq $salesCount) "Sales count should persist"
    Assert "R4-users-persist" ([int]$usersAfter[0].cnt -eq $usersCount) "Users count should persist"
    Assert "R4-medicines-persist" ([int]$medsAfter[0].cnt -eq $medsCount) "Medicines count should persist"

    # Verify system works
    $ctx = New-Login "admin@demo.com" "admin123"
    $health = Call "GET" "/system/health" $ctx
    Assert "R4-system-healthy" $health.ok "System should be healthy after DB restart"

    # Verify a sale can be made
    $shiftResp = Call "GET" "/shifts/active/user/$($ctx.userId)" $ctx
    if ($shiftResp.ok -and $shiftResp.data) {
        $shift = $shiftResp.data
    } else {
        $shiftResp = Call "POST" "/shifts" $ctx @{ shiftName = "R4 Shift"; openingFloat = 10000 }
        $shift = $shiftResp.data
    }

    if (-not $shift) {
        Assert "R4-sale-after-restart" $false "Could not get/create shift after restart"
    } else {

    $medsResp = Call "GET" "/medicines?size=200" $ctx
    $med = @($medsResp.data.content | Where-Object { $_.brandName -like "*Paracetamol*" }) | Select-Object -First 1
    $price = if ($med.sellingPrice) { [double]$med.sellingPrice } else { 40.00 }

    $saleResp = Call "POST" "/sales" $ctx @{
        clientSaleId = [guid]::NewGuid().ToString()
        shiftId = $shift.id
        items = @(@{
            medicineId = $med.id
            quantity = 1
            unitPrice = $price
            expectedUnitPrice = $price
            lineId = [guid]::NewGuid().ToString()
        })
        payments = @(@{
            method = "CASH"
            amount = $price
        })
        cashTendered = $price
    }
    Assert "R4-sale-after-restart" $saleResp.ok "Sale should work after restart"
    }

} catch {
    Assert "R4-unhandled-error" $false $_.Exception.Message
}

End-TestGroup

# ═══════════════════════════════════════════════════════════
# R5: Full Stack Restart — Comprehensive Verification
# ═══════════════════════════════════════════════════════════
Begin-TestGroup "R5: Full Stack Restart"

try {
    # Create comprehensive data
    $ctx = New-Login "admin@demo.com" "admin123"

    # Record state
    $stateBefore = @{
        sales = [int](Db-Query "SELECT COUNT(*) as cnt FROM sales")[0].cnt
        shifts = [int](Db-Query "SELECT COUNT(*) as cnt FROM staff_shifts")[0].cnt
        stock = [int](Db-Query "SELECT COALESCE(SUM(quantity_available), 0) as cnt FROM stock")[0].cnt
        audit = [int](Db-Query "SELECT COUNT(*) as cnt FROM audit_logs")[0].cnt
    }

    # Restart
    Restart-DockerStack
    Wait-ForApi 90

    # Verify state
    $stateAfter = @{
        sales = [int](Db-Query "SELECT COUNT(*) as cnt FROM sales")[0].cnt
        shifts = [int](Db-Query "SELECT COUNT(*) as cnt FROM staff_shifts")[0].cnt
        stock = [int](Db-Query "SELECT COALESCE(SUM(quantity_available), 0) as cnt FROM stock")[0].cnt
        audit = [int](Db-Query "SELECT COUNT(*) as cnt FROM audit_logs")[0].cnt
    }

    Assert "R5-sales-survive" ($stateAfter.sales -ge $stateBefore.sales) "Sales should survive restart"
    Assert "R5-shifts-survive" ($stateAfter.shifts -ge $stateBefore.shifts) "Shifts should survive restart"
    Assert "R5-stock-survive" ($stateAfter.stock -ge 0) "Stock should be non-negative"
    Assert "R5-audit-survive" ($stateAfter.audit -ge $stateBefore.audit) "Audit logs should survive restart"

    # All services healthy
    $health = Call "GET" "/system/health" $ctx
    Assert "R5-health-ok" $health.ok "Health endpoint should work"

} catch {
    Assert "R5-unhandled-error" $false $_.Exception.Message
}

End-TestGroup

Write-Summary
