package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** One row of the PLU (per-item) report: sold qty vs remaining stock. */
public record PluRowDto(
        UUID medicineId,
        String medicineName,
        String sku,
        BigDecimal unitPrice,
        long quantitySold,
        BigDecimal revenue,
        int remainingStock) {
}
