package com.example.pos.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayRequest {
    private String phoneNumber;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private String description;
    private String accountReference;
    private String email;
    private String callbackUrl;
    private Map<String, String> metadata;
}
