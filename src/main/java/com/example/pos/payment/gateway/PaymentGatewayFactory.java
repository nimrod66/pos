package com.example.pos.payment.gateway;

import com.example.pos.sale.payment.model.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayFactory {

    private final Map<String, PaymentGateway> gateways;

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(
                        g -> g.getType().toUpperCase(),
                        g -> g));
    }

    public PaymentGateway getGateway(String method) {
        PaymentGateway gateway = gateways.get(method.toUpperCase());
        if (gateway == null) {
            throw new IllegalArgumentException("No payment gateway configured for method: " + method);
        }
        return gateway;
    }

    public PaymentGateway getGateway(Payment.PaymentMethod method) {
        return getGateway(method.name());
    }

    public Map<String, String> listConfigured() {
        return gateways.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getClass().getSimpleName()));
    }
}
