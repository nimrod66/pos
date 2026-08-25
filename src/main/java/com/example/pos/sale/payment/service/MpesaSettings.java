package com.example.pos.sale.payment.service;

import com.example.pos.core.systemsettings.repository.SystemSettingsRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves M-Pesa Daraja credentials for the authenticated user's pharmacy.
 * Per-pharmacy values live in system_settings so each pharmacy configures its
 * own paybill from the UI; deployment-level environment variables act as a
 * fallback for pharmacies without overrides.
 */
@Component
public class MpesaSettings {

    public record Config(String consumerKey, String consumerSecret, String passkey,
                         String shortcode, String environment, String callbackUrl) {
        public boolean stkReady() {
            return notBlank(consumerKey) && notBlank(consumerSecret)
                    && notBlank(passkey) && notBlank(callbackUrl);
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }

    private final SystemSettingsRepository settingsRepository;
    private final AuthenticatedUserContext current;

    @Value("${mpesa.consumer-key:}")
    private String envConsumerKey;

    @Value("${mpesa.consumer-secret:}")
    private String envConsumerSecret;

    @Value("${mpesa.passkey:}")
    private String envPasskey;

    @Value("${mpesa.shortcode:174379}")
    private String envShortcode;

    @Value("${mpesa.environment:sandbox}")
    private String envEnvironment;

    @Value("${mpesa.callback-url:}")
    private String envCallbackUrl;

    public MpesaSettings(SystemSettingsRepository settingsRepository,
                         AuthenticatedUserContext current) {
        this.settingsRepository = settingsRepository;
        this.current = current;
    }

    public Config resolve() {
        UUID pharmacyId = null;
        try {
            pharmacyId = current.pharmacyId();
        } catch (RuntimeException ignored) {
            // Platform-level callers fall back to deployment defaults.
        }
        return new Config(
                value("payment.mpesa_consumer_key", envConsumerKey, pharmacyId),
                value("payment.mpesa_consumer_secret", envConsumerSecret, pharmacyId),
                value("payment.mpesa_passkey", envPasskey, pharmacyId),
                value("payment.mpesa_shortcode", envShortcode, pharmacyId),
                value("payment.mpesa_environment", envEnvironment, pharmacyId),
                value("payment.mpesa_callback_url", envCallbackUrl, pharmacyId));
    }

    private String value(String key, String fallback, UUID pharmacyId) {
        if (pharmacyId == null) return fallback;
        return settingsRepository.findSetting(key, null, pharmacyId)
                .map(setting -> setting.getSettingValue())
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }
}
