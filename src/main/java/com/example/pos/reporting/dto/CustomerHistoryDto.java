package com.example.pos.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerHistoryDto(
        UUID customerId,
        String customerName,
        String phoneNumber,
        Integer loyaltyPoints,
        int totalPurchases,
        BigDecimal totalSpent,
        List<CustomerSaleDto> recentSales,
        List<CustomerPrescriptionDto> prescriptions) {

    public record CustomerSaleDto(
            UUID saleId,
            LocalDateTime saleDate,
            BigDecimal total,
            String paymentMethod,
            List<CustomerSaleItemDto> items) {
    }

    public record CustomerSaleItemDto(
            String medicineName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal total) {
    }

    public record CustomerPrescriptionDto(
            UUID prescriptionId,
            String prescriptionNumber,
            String doctorName,
            String diagnosis,
            LocalDate issuedDate,
            String status,
            List<CustomerPrescriptionItemDto> items) {
    }

    public record CustomerPrescriptionItemDto(
            String medicineName,
            String dosage,
            int quantity) {
    }
}
