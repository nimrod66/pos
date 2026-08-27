package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SlowStockReportDto(
        UUID branchId,
        LocalDate asOf,
        int totalItems,
        int slowMovingCount,
        int deadStockCount,
        BigDecimal slowMovingValue,
        BigDecimal deadStockValue,
        List<SlowStockItemDto> items) {

    public record SlowStockItemDto(
            UUID medicineId,
            String medicineName,
            String sku,
            int currentStock,
            int soldLast90Days,
            int daysSinceLastSale,
            BigDecimal stockValue,
            String velocityCategory) {
    }
}
