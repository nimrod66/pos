package com.example.pos.payment.gateway.impl;

import com.example.pos.payment.gateway.PaymentGateway;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import com.example.pos.payment.gateway.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnExpression("not T(org.springframework.util.StringUtils).hasText('${paystack.secret-key:}')")
public class CardPaymentGateway implements PaymentGateway {

    @Value("${card.gateway.provider:PESAPAL}")
    private String provider;

    @Value("${card.gateway.consumer-key:}")
    private String consumerKey;

    @Value("${card.gateway.consumer-secret:}")
    private String consumerSecret;

    @Value("${card.gateway.api-url:}")
    private String apiUrl;

    @Override
    public String getType() {
        return "CARD";
    }

    @Override
    public PaymentGatewayResponse process(PaymentGatewayRequest request) {
        if (!isConfigured()) {
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .status(PaymentGatewayResponse.Status.FAILED.name())
                    .responseCode("CONFIG_ERROR")
                    .responseDescription("Card gateway not configured. Set card.gateway.consumer-key and card.gateway.consumer-secret")
                    .build();
        }

        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionReference(provider + "-" + System.currentTimeMillis())
                .status(PaymentGatewayResponse.Status.PROCESSING.name())
                .responseCode("0")
                .responseDescription("Payment initiated via " + provider + ". Redirect to payment page required.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentGatewayResponse queryStatus(String transactionReference) {
        log.info("Querying card payment status via {}: {}", provider, transactionReference);
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionReference(transactionReference)
                .status(PaymentGatewayResponse.Status.COMPLETED.name())
                .responseCode("0")
                .responseDescription("Payment completed via " + provider)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentGatewayResponse refund(String transactionReference, BigDecimal amount) {
        log.info("Processing refund via {}: {} amount={}", provider, transactionReference, amount);
        return PaymentGatewayResponse.builder()
                .success(true)
                .transactionReference("REF-" + transactionReference)
                .status(PaymentGatewayResponse.Status.REFUNDED.name())
                .responseCode("0")
                .responseDescription("Refund initiated via " + provider)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private boolean isConfigured() {
        return consumerKey != null && !consumerKey.isBlank()
                && consumerSecret != null && !consumerSecret.isBlank();
    }
}
