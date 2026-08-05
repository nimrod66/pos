package com.example.pos.finance.shiftreport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ShiftReport(
        UUID shiftId,
        String shiftName,
        String branchName,
        String cashierName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String status,
        BigDecimal openingBalance,
        BigDecimal expectedClosingBalance,
        BigDecimal actualClosingBalance,
        BigDecimal variance,
        int salesCount,
        BigDecimal totalSales,
        BigDecimal totalCashPayments,
        BigDecimal totalMpesaPayments,
        BigDecimal totalCardPayments,
        BigDecimal totalExpenses,
        BigDecimal totalRefunds,
        int refundCount,
        List<Map<String, Object>> paymentBreakdown,
        List<Map<String, Object>> expenseBreakdown,
        List<Map<String, Object>> refundBreakdown
) {}
