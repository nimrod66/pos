package com.example.pos.compliance.invoice.dto;

import java.math.BigDecimal;

public record SaleFiscalItemData(
        Long medicineId,
        String medicineName,
        String barcode,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal taxAmount,
        BigDecimal taxRate,
        String taxCode,
        String taxType
) {}