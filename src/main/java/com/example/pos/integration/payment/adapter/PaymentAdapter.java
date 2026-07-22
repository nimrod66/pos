package com.example.pos.integration.payment.adapter;

import com.example.pos.integration.payment.dto.v1.PaymentRequest;
import com.example.pos.integration.payment.dto.v1.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentAdapter {

    String getMethod();

    PaymentResponse process(PaymentRequest request);

    PaymentResponse queryStatus(String transactionReference);

    PaymentResponse refund(String transactionReference, BigDecimal amount);
}
