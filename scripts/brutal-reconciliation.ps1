#Requires -Version 5.1
# brutal-reconciliation.ps1 — Full data consistency audit.
# Run AFTER all other brutal tests. Sources brutal-common.ps1.
. "$PSScriptRoot\brutal-common.ps1"

Write-Host ""
Write-Host "========================================================" -ForegroundColor White
Write-Host "  BRUTAL RECONCILIATION — FULL DATA CONSISTENCY AUDIT" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor White

$ownerCtx = New-Login "admin@demo.com" "admin123"
if (-not $ownerCtx.session) {
    Write-Host "FATAL: Cannot login as owner" -ForegroundColor Red
    exit 1
}
$me = Call "GET" "/auth/me" $ownerCtx
$branchId = $me.data.user.activeBranch.id
$pharmacyId = $me.data.user.pharmacyId
$today = Get-Date -Format "yyyy-MM-dd"

# ─────────────────────────────────────────────────────────
# Rec1: Sales vs Payments
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Rec1-SalesVsPayments"

$salesMismatch = @()
$dbSales = Db-Query "SELECT id, total, status FROM sales WHERE branch_id = '$branchId' AND status = 'COMPLETED' ORDER BY completed_at DESC LIMIT 100"
foreach ($sale in $dbSales) {
    $saleId = $sale.col0
    $saleTotal = [double]$sale.col1

    $paymentSum = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE sale_id = '$saleId'"
    $diff = [math]::Round([double]$paymentSum - $saleTotal, 2)

    if ([math]::Abs($diff) -gt 0.01) {
        $salesMismatch += @{ saleId = $saleId; expected = $saleTotal; actual = [double]$paymentSum; diff = $diff }
    }
}

$salesReconciled = $salesMismatch.Count -eq 0
Assert "Rec1-Sales-Payments-Match" $salesReconciled "Found $($salesMismatch.Count) mismatches"
if (-not $salesReconciled) {
    Write-Host "  MISMATCHES:" -ForegroundColor Red
    foreach ($m in $salesMismatch) {
        Write-Host ("    Sale {0}: expected={1} actual={2} diff={3}" -f $m.saleId, $m.expected, $m.actual, $m.diff) -ForegroundColor Red
    }
}

$saleCount = Db-Scalar "SELECT COUNT(*) FROM sales WHERE branch_id = '$branchId' AND status = 'COMPLETED'"
Record "Rec1-Sales-Count" "PASS" "Audited $saleCount sales"

End-TestGroup

# ─────────────────────────────────────────────────────────
# Rec2: Inventory vs Movements
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Rec2-InventoryVsMovements"

$stockMismatch = @()
$dbStock = Db-Query "SELECT medicine_id, SUM(quantity_available) as qty FROM medicine_batches WHERE branch_id = '$branchId' AND quantity_available > 0 GROUP BY medicine_id"

foreach ($row in $dbStock) {
    $medId = $row.col0
    $actualQty = [int]$row.col1

    $movementSum = Db-Scalar "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE medicine_id = '$medId' AND branch_id = '$branchId'"
    $purchased = Db-Scalar "SELECT COALESCE(SUM(quantity), 0) FROM goods_received_lines grl JOIN goods_received gr ON grl.goods_received_id = gr.id WHERE grl.medicine_id = '$medId' AND gr.branch_id = '$branchId'"
    $sold = Db-Scalar "SELECT COALESCE(SUM(sa.quantity), 0) FROM sale_allocations sa JOIN sales s ON sa.sale_id = s.id WHERE sa.medicine_id = '$medId' AND s.branch_id = '$branchId' AND s.status = 'COMPLETED'"
    $returned = Db-Scalar "SELECT COALESCE(SUM(ri.quantity), 0) FROM return_items ri JOIN sale_returns sr ON ri.return_id = sr.id JOIN sales s ON sr.sale_id = s.id WHERE ri.medicine_batches_id IN (SELECT id FROM medicine_batches WHERE medicine_id = '$medId') AND s.branch_id = '$branchId'"
    $disposed = Db-Scalar "SELECT COALESCE(SUM(quantity_disposed), 0) FROM expiry_logs el JOIN medicine_batches mb ON el.medicine_batches_id = mb.id WHERE mb.medicine_id = '$medId' AND mb.branch_id = '$branchId'"

    $expectedQty = [int]$purchased - [int]$sold + [int]$returned - [int]$disposed
    $qtyDiff = [int]$actualQty - $expectedQty

    if ([math]::Abs($qtyDiff) -gt 0) {
        $stockMismatch += @{ medId = $medId; expected = $expectedQty; actual = $actualQty; diff = $qtyDiff }
    }
}

$stockReconciled = $stockMismatch.Count -eq 0
Assert "Rec2-Inventory-Movements-Match" $stockReconciled "Found $($stockMismatch.Count) mismatches"
if (-not $stockReconciled) {
    Write-Host "  MISMATCHES:" -ForegroundColor Red
    foreach ($m in $stockMismatch) {
        Write-Host ("    Medicine {0}: expected={1} actual={2} diff={3}" -f $m.medId, $m.expected, $m.actual, $m.diff) -ForegroundColor Red
    }
}

$medCount = Db-Scalar "SELECT COUNT(DISTINCT medicine_id) FROM medicine_batches WHERE branch_id = '$branchId' AND quantity_available > 0"
Record "Rec2-Medicine-Count" "PASS" "Audited $medCount medicines"

End-TestGroup

# ─────────────────────────────────────────────────────────
# Rec3: Cash Drawer vs Transactions
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Rec3-CashDrawerVsTransactions"

$drawerMismatch = @()
$closedShifts = Db-Query "SELECT id, opening_float, actual_cash, expected_cash, variance FROM staff_shifts WHERE status = 'CLOSED' ORDER BY closed_at DESC LIMIT 50"

foreach ($shift in $closedShifts) {
    $sId = $shift.col0
    $openingFloat = [double]$shift.col1
    $actualCash = [double]$shift.col2
    $apiExpected = [double]$shift.col3

    $cashIn = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE shift_id = '$sId' AND transaction_type = 'CASH_IN'"
    $cashOut = Db-Scalar "SELECT COALESCE(SUM(amount), 0) FROM cash_transactions WHERE shift_id = '$sId' AND transaction_type = 'CASH_OUT'"
    $cashSales = Db-Scalar "SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN sales s ON p.sale_id = s.id WHERE s.shift_id = '$sId' AND p.method = 'CASH' AND s.status = 'COMPLETED'"
    $cashRefunds = Db-Scalar "SELECT COALESCE(SUM(sr.refund_amount), 0) FROM sale_returns sr JOIN sales s ON sr.sale_id = s.id WHERE s.shift_id = '$sId' AND sr.refund_method = 'CASH' AND sr.status = 'COMPLETED'"

    $dbExpected = $openingFloat + [double]$cashIn - [double]$cashOut + [double]$cashSales - [double]$cashRefunds
    $diff = [math]::Round($apiExpected - $dbExpected, 2)

    if ([math]::Abs($diff) -gt 0.01) {
        $drawerMismatch += @{ shiftId = $sId; apiExpected = $apiExpected; dbExpected = [math]::Round($dbExpected, 2); diff = $diff }
    }
}

$drawerReconciled = $drawerMismatch.Count -eq 0
Assert "Rec3-Drawer-Transactions-Match" $drawerReconciled "Found $($drawerMismatch.Count) mismatches"
if (-not $drawerReconciled) {
    Write-Host "  MISMATCHES:" -ForegroundColor Red
    foreach ($m in $drawerMismatch) {
        Write-Host ("    Shift {0}: api={1} db={2} diff={3}" -f $m.shiftId, $m.apiExpected, $m.dbExpected, $m.diff) -ForegroundColor Red
    }
}

$shiftCount = @($closedShifts).Count
Record "Rec3-Shift-Count" "PASS" "Audited $shiftCount closed shifts"

End-TestGroup

# ─────────────────────────────────────────────────────────
# Rec4: Audit Trail Completeness
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Rec4-AuditTrailCompleteness"

$auditResult = Reconcile-Audit $today $today
Assert "Rec4-Audit-Count-Match" $auditResult.ok "Audit count mismatch: API=$($auditResult.actual.count) DB=$($auditResult.expected.count)"

$requiredActions = @(
    "CREATE_SALE",
    "OPEN_SHIFT",
    "LOGIN"
)

$auditActions = Db-Query "SELECT DISTINCT action FROM audit_logs WHERE DATE(created_at) = '$today'"
$existingActions = @($auditActions | ForEach-Object { $_.col0 })

$missingActions = @()
foreach ($action in $requiredActions) {
    if ($existingActions -notcontains $action) {
        $missingActions += $action
    }
}

$auditComplete = $missingActions.Count -eq 0
Assert "Rec4-Required-Audit-Actions" $auditComplete "Missing audit actions: $($missingActions -join ', ')"
if (-not $auditComplete) {
    Write-Host "  MISSING ACTIONS:" -ForegroundColor Red
    foreach ($a in $missingActions) {
        Write-Host "    - $a" -ForegroundColor Red
    }
}

$saleAuditCount = Db-Scalar "SELECT COUNT(*) FROM audit_logs WHERE action = 'CREATE_SALE' AND DATE(created_at) = '$today'"
$shiftAuditCount = Db-Scalar "SELECT COUNT(*) FROM audit_logs WHERE action IN ('OPEN_SHIFT', 'CLOSE_SHIFT') AND DATE(created_at) = '$today'"
Record "Rec4-Audit-Summary" "PASS" "Sales entries: $saleAuditCount, Shift entries: $shiftAuditCount"

End-TestGroup

# ─────────────────────────────────────────────────────────
# Rec5: Prescription Consistency
# ─────────────────────────────────────────────────────────
Begin-TestGroup "Rec5-PrescriptionConsistency"

$dispensedWithoutSale = Db-Query "SELECT p.id, p.prescription_number FROM prescriptions p WHERE p.status = 'DISPENSED' AND NOT EXISTS (SELECT 1 FROM sales s WHERE s.prescription_reference_id = p.id) LIMIT 10"
$orphanCount = @($dispensedWithoutSale).Count
Assert "Rec5-Dispensed-Have-Sales" ($orphanCount -eq 0) "Found $orphanCount dispensed prescriptions without matching sales"
if ($orphanCount -gt 0) {
    Write-Host "  ORPHANED PRESCRIPTIONS:" -ForegroundColor Red
    foreach ($p in $dispensedWithoutSale) {
        Write-Host ("    Prescription {0} ({1})" -f $p.col0, $p.col1) -ForegroundColor Red
    }
}

$overDispensed = Db-Query "SELECT pi.id, pi.quantity as prescribed, COALESCE(SUM(sa.quantity), 0) as dispensed FROM prescription_items pi JOIN prescriptions p ON pi.prescription_id = p.id LEFT JOIN sale_allocations sa ON sa.prescription_item_id = pi.id WHERE p.status = 'DISPENSED' GROUP BY pi.id, pi.quantity HAVING COALESCE(SUM(sa.quantity), 0) > pi.quantity LIMIT 10"
$overCount = @($overDispensed).Count
Assert "Rec5-Prescription-Quantities-Valid" ($overCount -eq 0) "Found $overCount over-dispensed prescriptions"
if ($overCount -gt 0) {
    Write-Host "  OVER-DISPENSED:" -ForegroundColor Red
    foreach ($p in $overDispensed) {
        Write-Host ("    Item {0}: prescribed={1} dispensed={2}" -f $p.col0, $p.col1, $p.col2) -ForegroundColor Red
    }
}

$rxCount = Db-Scalar "SELECT COUNT(*) FROM prescriptions WHERE status = 'DISPENSED'"
Record "Rec5-Prescription-Count" "PASS" "Audited $rxCount dispensed prescriptions"

End-TestGroup

# ─────────────────────────────────────────────────────────
# Output and summary
# ─────────────────────────────────────────────────────────
Write-Summary

$reportPath = Join-Path $PSScriptRoot "brutal-reconciliation-results.json"
Get-TestReport | Out-File -FilePath $reportPath -Encoding UTF8
Write-Host "`nResults written to: $reportPath" -ForegroundColor DarkGray

if ($script:FailCount -gt 0) { exit 1 }
