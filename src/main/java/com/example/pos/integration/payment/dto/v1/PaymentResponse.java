package com.example.pos.integration.payment.dto.v1;

import java.math.BigDecimal;

public record PaymentResponse(
        boolean success,
        String transactionReference,
        String status,
        String responseCode,
        String responseDescription,
        String rawResponse
) {
    public static PaymentResponse ok(String transactionReference, String status, String description) {
        return new PaymentResponse(true, transactionReference, status, "OK", description, null);
    }

    public static PaymentResponse pending(String transactionReference, String description) {
        return new PaymentResponse(true, transactionReference, "PENDING", "PENDING", description, null);
    }

    public static PaymentResponse error(String code, String description) {
        return new PaymentResponse(false, null, "FAILED", code, description, null);
    }
}
