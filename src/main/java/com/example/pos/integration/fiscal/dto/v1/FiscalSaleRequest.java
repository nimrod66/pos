package com.example.pos.integration.fiscal.dto.v1;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FiscalSaleRequest(
        UUID saleId,
        String branchCode,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discount,
        BigDecimal grandTotal,
        String currency,
        String customerPin,
        String customerName,
        boolean cancelled,
        List<FiscalSaleItemRequest> items
) {
    public record FiscalSaleItemRequest(
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
}
