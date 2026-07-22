package com.example.pos.integration.sms;

import com.example.pos.integration.config.FeatureFlagService;
import com.example.pos.integration.sms.dto.v1.SmsRequest;
import com.example.pos.integration.sms.dto.v1.SmsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "pos.features.sms", havingValue = "true")
public class AfricasTalkingSmsAdapter implements SmsAdapter {

    private static final Logger log = LoggerFactory.getLogger(AfricasTalkingSmsAdapter.class);
    private static final String AT_API_URL = "https://api.africastalking.com/version1/messaging";

    private final RestClient restClient;
    private final FeatureFlagService featureFlags;

    @Value("${africastalking.api-key:}")
    private String apiKey;

    @Value("${africastalking.username:}")
    private String username;

    @Value("${africastalking.sender-id:}")
    private String defaultSenderId;

    public AfricasTalkingSmsAdapter(FeatureFlagService featureFlags) {
        this.featureFlags = featureFlags;
        this.restClient = RestClient.builder()
                .baseUrl(AT_API_URL)
                .defaultHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Override
    public SmsResponse send(SmsRequest request) {
        if (!featureFlags.isSmsEnabled()) {
            return SmsResponse.fail("SMS feature disabled");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return SmsResponse.fail("Africa's Talking API key not configured");
        }

        try {
            String senderId = request.senderId() != null ? request.senderId() : defaultSenderId;
            if (senderId == null) senderId = "POS";

            Map<String, String> formData = new LinkedHashMap<>();
            formData.put("username", username != null ? username : "sandbox");
            formData.put("to", request.to());
            formData.put("message", request.message());
            if (senderId != null) formData.put("from", senderId);

            var response = restClient.post()
                    .header("apiKey", apiKey)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            log.info("SMS sent to {}: {}", request.to(), request.message());
            return SmsResponse.ok(UUID.randomUUID().toString());

        } catch (Exception e) {
            log.error("SMS send failed to {}: {}", request.to(), e.getMessage());
            return SmsResponse.fail(e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return featureFlags.isSmsEnabled() && apiKey != null && !apiKey.isBlank();
    }
}
