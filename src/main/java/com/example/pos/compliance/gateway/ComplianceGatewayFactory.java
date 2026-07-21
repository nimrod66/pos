package com.example.pos.compliance.gateway;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ComplianceGatewayFactory {

    private final Map<String, ComplianceGateway> gateways;

    public ComplianceGatewayFactory(List<ComplianceGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(
                        g -> g.getProviderName().toUpperCase(),
                        Function.identity()));
    }

    public ComplianceGateway getGateway(String providerCode) {
        ComplianceGateway gw = gateways.get(providerCode.toUpperCase());
        if (gw == null) {
            throw new IllegalArgumentException("No compliance gateway registered for provider: " + providerCode);
        }
        return gw;
    }

    public ComplianceGateway getForInvoice(TaxInvoice invoice) {
        return getGateway("OSCU");
    }

    public boolean hasProvider(String providerCode) {
        return gateways.containsKey(providerCode.toUpperCase());
    }
}
