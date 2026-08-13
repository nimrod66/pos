package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalesReportDto(
        UUID branchId,
        boolean pharmacyWide,
        LocalDate from,
        LocalDate to,
        int completedSalesCount,
        BigDecimal grossSales,
        BigDecimal refunds,
        BigDecimal netSales,
        BigDecimal cashPayments,
        BigDecimal mpesaPayments,
        BigDecimal otherPayments,
        BigDecimal cashRefunds,
        BigDecimal mpesaRefunds,
        BigDecimal otherRefunds,
        List<TopProductDto> topProducts) {

    public record TopProductDto(
            UUID medicineId,
            String medicineName,
            int quantity,
            BigDecimal netRevenue) {
    }
}
