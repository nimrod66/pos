package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DashboardReportDto(
        UUID branchId,
        boolean pharmacyWide,
        LocalDate date,
        int completedSalesCount,
        BigDecimal grossSales,
        BigDecimal refunds,
        BigDecimal netSales,
        int lowStockCount,
        int totalStockItems,
        int nearExpiryCount,
        int expiredCount,
        int nearExpiryDays) {
}
