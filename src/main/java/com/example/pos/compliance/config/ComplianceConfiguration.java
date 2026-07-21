package com.example.pos.compliance.config;

import com.example.pos.compliance.gateway.model.ComplianceMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "compliance")
@Data
public class ComplianceConfiguration {

    private ComplianceMode mode = ComplianceMode.MOCK;

    private String kraPin;

    private String deviceSerial;

    private String activeProvider = "OSCU";

    private String certificatePath;

    private boolean strictValidation = false;

    private boolean logFullPayloads = true;

    private int maxRetryAttempts = 10;

    private long retryIntervalMs = 60000;

    public boolean isProduction() {
        return mode == ComplianceMode.PRODUCTION;
    }

    public boolean isCertification() {
        return mode == ComplianceMode.CERTIFICATION;
    }

    public boolean isSandbox() {
        return mode == ComplianceMode.SANDBOX;
    }

    public boolean isMock() {
        return mode == ComplianceMode.MOCK;
    }
}
