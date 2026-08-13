package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryReportDto(
        UUID branchId,
        boolean pharmacyWide,
        LocalDate asOf,
        BigDecimal stockValue,
        int lowStockCount,
        int batchCount,
        int nearExpiryCount,
        int expiredCount,
        int nearExpiryDays,
        List<LowStockItemDto> lowStockItems) {

    public record LowStockItemDto(
            UUID branchId,
            String branchName,
            UUID medicineId,
            String medicineName,
            String sku,
            int available,
            int reorderLevel) {
    }
}
