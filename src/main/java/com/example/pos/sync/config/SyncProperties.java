package com.example.pos.sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pos.sync")
public class SyncProperties {
    private String mode = "local";
    private String centralUrl = "http://localhost:9090";
    private int pollIntervalSeconds = 30;

    public boolean isEnabled() {
        return "hybrid".equalsIgnoreCase(mode);
    }
}
