package com.example.pos.integration.payment.adapter;

import com.example.pos.integration.payment.dto.v1.PaymentRequest;
import com.example.pos.integration.payment.dto.v1.PaymentResponse;
import com.example.pos.payment.gateway.PaymentGatewayFactory;
import com.example.pos.payment.gateway.PaymentGatewayRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MpesaPaymentAdapter implements PaymentAdapter {

    private final PaymentGatewayFactory factory;

    public MpesaPaymentAdapter(PaymentGatewayFactory factory) {
        this.factory = factory;
    }

    @Override
    public String getMethod() { return "M_PESA"; }

    @Override
    public PaymentResponse process(PaymentRequest request) {
        var r = factory.getGateway("M_PESA").process(toGwRequest(request));
        return mapResponse(r);
    }

    @Override
    public PaymentResponse queryStatus(String ref) {
        var r = factory.getGateway("M_PESA").queryStatus(ref);
        return mapResponse(r);
    }

    @Override
    public PaymentResponse refund(String ref, BigDecimal amount) {
        var r = factory.getGateway("M_PESA").refund(ref, amount);
        return mapResponse(r);
    }

    private PaymentGatewayRequest toGwRequest(PaymentRequest r) {
        return PaymentGatewayRequest.builder()
                .amount(r.amount()).currency(r.currency()).reference(r.reference())
                .description(r.description()).phoneNumber(r.phoneNumber())
                .email(r.email()).callbackUrl(r.callbackUrl()).build();
    }

    private PaymentResponse mapResponse(com.example.pos.payment.gateway.PaymentGatewayResponse r) {
        return new PaymentResponse(r.isSuccess(), r.getTransactionReference(),
                r.getStatus(), r.getResponseCode(), r.getResponseDescription(), r.getRawResponse());
    }
}
