package com.example.pos.compliance.transmission.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransmissionScheduler.class);

    private final TransmissionService transmissionService;

    public TransmissionScheduler(TransmissionService transmissionService) {
        this.transmissionService = transmissionService;
    }

    @Scheduled(fixedDelayString = "${compliance.retry-interval-ms:60000}")
    public void retryFailedTransmissions() {
        try {
            log.debug("Scheduled retry of failed transmissions");
            transmissionService.requeueFailed();
        } catch (Exception e) {
            log.error("Failed to retry transmissions", e);
        }
    }
}
