package com.example.pos.compliance.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleFiscalItemData(
        UUID medicineId,
        String medicineName,
        String barcode,
        String barcodeType,
        String etimsClassificationCode,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal taxAmount,
        BigDecimal taxRate,
        String taxCode,
        String taxType
) {}