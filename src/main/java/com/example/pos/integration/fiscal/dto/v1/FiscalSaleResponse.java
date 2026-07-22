package com.example.pos.integration.fiscal.dto.v1;

import java.time.LocalDateTime;

public record FiscalSaleResponse(
        Long invoiceId,
        String invoiceNumber,
        String status,
        String kraReceiptNumber,
        String qrCodeContent,
        String verificationUrl,
        LocalDateTime issuedAt
) {}
