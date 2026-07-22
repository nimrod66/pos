package com.example.pos.integration.config;

import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final FeatureFlags flags;

    public FeatureFlagService(FeatureFlags flags) {
        this.flags = flags;
    }

    public boolean isEnabled(String feature) {
        return switch (feature.toLowerCase()) {
            case "fiscal" -> flags.isFiscal();
            case "payment" -> flags.isPayment();
            case "devices" -> flags.isDevices();
            case "offline" -> flags.isOffline();
            case "email" -> flags.isEmail();
            case "sms" -> flags.isSms();
            default -> false;
        };
    }

    public boolean isFiscalEnabled() { return flags.isFiscal(); }
    public boolean isPaymentEnabled() { return flags.isPayment(); }
    public boolean isDevicesEnabled() { return flags.isDevices(); }
    public boolean isOfflineEnabled() { return flags.isOffline(); }
    public boolean isEmailEnabled() { return flags.isEmail(); }
    public boolean isSmsEnabled() { return flags.isSms(); }
}
