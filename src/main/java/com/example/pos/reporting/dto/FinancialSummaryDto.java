package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialSummaryDto(
    UUID branchId,
    boolean pharmacyWide,
    LocalDate from,
    LocalDate to,
    // Income
    BigDecimal grossSales,
    BigDecimal salesReturns,
    BigDecimal netSales,
    // Cost of goods sold
    BigDecimal totalCostOfGoodsSold,
    BigDecimal grossProfit,
    BigDecimal grossMarginPercent,
    // Expenses
    BigDecimal totalExpenses,
    List<ExpenseByCategory> expensesByCategory,
    // Net profit
    BigDecimal netProfit,
    BigDecimal netProfitMarginPercent,
    // Payment breakdown
    BigDecimal cashCollected,
    BigDecimal mpesaCollected,
    BigDecimal creditCollected
) {
    public record ExpenseByCategory(
        UUID categoryId,
        String categoryName,
        BigDecimal totalAmount,
        int transactionCount
    ) {}
}
