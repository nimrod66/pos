package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProfitReportDto(
        UUID branchId,
        boolean pharmacyWide,
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        BigDecimal totalCostOfGoodsSold,
        BigDecimal grossProfit,
        BigDecimal grossMarginPercent,
        List<MedicineProfitDto> medicineBreakdown) {

    public record MedicineProfitDto(
            UUID medicineId,
            String medicineName,
            String sku,
            int quantitySold,
            BigDecimal revenue,
            BigDecimal costOfGoods,
            BigDecimal profit,
            BigDecimal marginPercent) {
    }
}
