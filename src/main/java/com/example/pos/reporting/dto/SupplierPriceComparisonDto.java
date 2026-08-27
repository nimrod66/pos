package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierPriceComparisonDto(
        UUID medicineId,
        String medicineName,
        String sku,
        List<SupplierPriceDto> suppliers) {

    public record SupplierPriceDto(
            UUID supplierId,
            String supplierName,
            BigDecimal lastUnitCost,
            int totalQuantityPurchased,
            int purchaseCount,
            LocalDate lastPurchaseDate,
            BigDecimal averageCost) {
    }
}
