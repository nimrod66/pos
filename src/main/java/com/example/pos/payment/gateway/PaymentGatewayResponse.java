package com.example.pos.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResponse {
    private boolean success;
    private String transactionReference;
    private String status;
    private String responseCode;
    private String responseDescription;
    private String merchantRequestId;
    private String checkoutRequestId;
    private String rawResponse;
    private LocalDateTime timestamp;

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    }
}
