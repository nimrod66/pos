package com.example.pos.integration.fiscal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pos.fiscal")
@Data
public class FiscalProperties {

    private boolean enabled = true;

    private FiscalMode mode = FiscalMode.LOCAL;

    private String remoteUrl;

    private String apiKey;

    private final Retry retry = new Retry();

    @Data
    public static class Retry {
        private int maxAttempts = 5;
        private long initialBackoffMs = 5000;
        private long maxBackoffMs = 300000;
        private long pollIntervalMs = 30000;
    }
}
