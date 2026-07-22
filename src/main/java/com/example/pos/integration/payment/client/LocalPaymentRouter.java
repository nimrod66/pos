package com.example.pos.integration.payment.client;

import com.example.pos.integration.payment.adapter.*;
import com.example.pos.integration.payment.dto.v1.PaymentRequest;
import com.example.pos.integration.payment.dto.v1.PaymentResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocalPaymentRouter implements PaymentClient {

    private final Map<String, PaymentAdapter> adapters;

    public LocalPaymentRouter(List<PaymentAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(
                        a -> a.getMethod().toUpperCase(),
                        a -> a));
    }

    @Override
    public PaymentResponse process(PaymentRequest request) {
        PaymentAdapter adapter = adapters.get(request.method().toUpperCase());
        if (adapter == null) {
            return PaymentResponse.error("UNSUPPORTED_METHOD", "No adapter for: " + request.method());
        }
        return adapter.process(request);
    }

    @Override
    public PaymentResponse queryStatus(String transactionReference, String method) {
        PaymentAdapter adapter = adapters.get(method.toUpperCase());
        if (adapter == null) {
            return PaymentResponse.error("UNSUPPORTED_METHOD", "No adapter for: " + method);
        }
        return adapter.queryStatus(transactionReference);
    }

    @Override
    public PaymentResponse refund(String transactionReference, BigDecimal amount, String method) {
        PaymentAdapter adapter = adapters.get(method.toUpperCase());
        if (adapter == null) {
            return PaymentResponse.error("UNSUPPORTED_METHOD", "No adapter for: " + method);
        }
        return adapter.refund(transactionReference, amount);
    }

    public List<String> configuredMethods() {
        return List.copyOf(adapters.keySet());
    }
}
