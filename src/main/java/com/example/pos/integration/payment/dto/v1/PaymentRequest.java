package com.example.pos.integration.payment.dto.v1;

import java.math.BigDecimal;

public record PaymentRequest(
        String method,
        BigDecimal amount,
        String currency,
        String reference,
        String description,
        String phoneNumber,
        String email,
        String callbackUrl
) {}
