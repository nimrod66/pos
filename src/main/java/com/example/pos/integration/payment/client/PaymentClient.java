package com.example.pos.integration.payment.client;

import com.example.pos.integration.payment.dto.v1.PaymentRequest;
import com.example.pos.integration.payment.dto.v1.PaymentResponse;

public interface PaymentClient {

    PaymentResponse process(PaymentRequest request);

    PaymentResponse queryStatus(String transactionReference, String method);

    PaymentResponse refund(String transactionReference, java.math.BigDecimal amount, String method);
}
