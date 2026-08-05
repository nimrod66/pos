package com.example.pos.integration.fiscal.snapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FiscalSaleSnapshot(
        UUID saleId,
        String invoiceNumber,
        String receiptNumber,
        String branchName,
        String cashierName,
        String cashierId,
        String businessName,
        String businessAddress,
        String businessPhone,
        String kraPin,
        String qrCodeContent,
        String verificationUrl,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal tax,
        BigDecimal total,
        String currency,
        LocalDateTime saleDate,
        String customerName,
        String customerPin,
        String terminalId,
        String terminalName,
        String terminalType,
        List<SnapshotItem> items,
        List<SnapshotPayment> payments
) {
    public record SnapshotItem(
            int lineNumber,
            String medicineName,
            String barcode,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            String taxCode,
            BigDecimal taxAmount,
            BigDecimal total
    ) {}

    public record SnapshotPayment(
            String method,
            BigDecimal amount,
            String reference
    ) {}
}
