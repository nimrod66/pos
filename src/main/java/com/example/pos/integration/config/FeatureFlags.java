package com.example.pos.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pos.features")
@Data
public class FeatureFlags {

    private boolean fiscal = true;
    private boolean payment = true;
    private boolean devices = true;
    private boolean offline = true;
    private boolean email = false;
    private boolean sms = false;
}
