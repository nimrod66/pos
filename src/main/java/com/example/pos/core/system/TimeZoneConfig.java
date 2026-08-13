package com.example.pos.core.system;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    private final ZoneId zoneId;

    public TimeZoneConfig(@Value("${spring.jackson.time-zone:Africa/Nairobi}") String zoneId) {
        this.zoneId = ZoneId.of(zoneId);
    }

    @PostConstruct
    void applyApplicationTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }

    @Bean
    Clock applicationClock() {
        return Clock.system(zoneId);
    }
}
