package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReorderSuggestionReportDto(
        UUID branchId,
        int totalMedicines,
        int needReorder,
        List<ReorderItemDto> items) {

    public record ReorderItemDto(
            UUID medicineId,
            String medicineName,
            String sku,
            int currentStock,
            int reorderLevel,
            int soldLast30Days,
            int suggestedOrderQty,
            BigDecimal estimatedCost,
            String urgency) {
    }
}
