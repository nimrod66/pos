package com.example.pos.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {

    String getType();

    default boolean supports(String method) {
        return getType().equalsIgnoreCase(method);
    }

    PaymentGatewayResponse process(PaymentGatewayRequest request);

    PaymentGatewayResponse queryStatus(String transactionReference);

    PaymentGatewayResponse refund(String transactionReference, BigDecimal amount);
}
