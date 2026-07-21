package com.example.pos.payment.gateway.impl;

import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CashPaymentGateway implements PaymentGateway {

    @Override
    public String getType() {
        return "CASH";
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request) {
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionReference("CASH-" + System.currentTimeMillis())
                .status(PaymentGatewayResponse.Status.COMPLETED.name())
                .responseCode("0")
                .responseDescription("Cash payment recorded")
                .build();
    }

    @Override
    public PaymentGatewayResponse queryStatus(String transactionReference) {
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionReference(transactionReference)
                .status(PaymentGatewayResponse.Status.COMPLETED.name())
                .responseCode("0")
                .responseDescription("Cash payment — always complete")
                .build();
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionReference("REF-CASH-" + System.currentTimeMillis())
                .status(PaymentGatewayResponse.Status.REFUNDED.name())
                .responseCode("0")
                .responseDescription("Cash refund processed")
                .build();
    }
}
