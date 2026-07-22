package com.example.pos.integration.payment.monitoring;

import com.example.pos.integration.payment.client.LocalPaymentRouter;
import com.example.pos.integration.config.FeatureFlagService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentHealthService {

    private final FeatureFlagService featureFlags;
    private final LocalPaymentRouter router;

    public PaymentHealthService(FeatureFlagService featureFlags, LocalPaymentRouter router) {
        this.featureFlags = featureFlags;
        this.router = router;
    }

    public Map<String, Object> check() {
        boolean enabled = featureFlags.isPaymentEnabled();
        return Map.of(
                "enabled", enabled,
                "configuredMethods", enabled ? router.configuredMethods() : java.util.List.of(),
                "status", enabled ? "ACTIVE" : "DISABLED"
        );
    }
}
