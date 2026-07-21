package com.example.pos.compliance.health;

import com.example.pos.compliance.health.ComplianceHealthIndicator;
import com.example.pos.compliance.config.ComplianceConfiguration;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import com.example.pos.compliance.transmission.repository.DeadLetterRecordRepository;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ComplianceHealthService {

    private final ComplianceHealthIndicator healthIndicator;
    private final ComplianceConfiguration config;
    private final TransmissionRepository transmissionRepo;
    private final DeadLetterRecordRepository deadLetterRepo;

    public ComplianceHealthService(ComplianceHealthIndicator healthIndicator,
                                   ComplianceConfiguration config,
                                   TransmissionRepository transmissionRepo,
                                   DeadLetterRecordRepository deadLetterRepo) {
        this.healthIndicator = healthIndicator;
        this.config = config;
        this.transmissionRepo = transmissionRepo;
        this.deadLetterRepo = deadLetterRepo;
    }

    public Map<String, Object> getHealthSnapshot() {
        long pending = transmissionRepo.countByTransmissionStatus(TransmissionStatus.PENDING);
        long failed = transmissionRepo.countByTransmissionStatus(TransmissionStatus.FAILED);
        long transmitted = transmissionRepo.countByTransmissionStatus(TransmissionStatus.TRANSMITTED);
        long dead = deadLetterRepo.countByStatus(
                com.example.pos.compliance.transmission.model.DeadLetterRecord.DeadLetterStatus.PENDING);

        return Map.of(
                "mode", config.getMode().name(),
                "provider", config.getActiveProvider(),
                "pending", pending,
                "failed", failed,
                "transmitted", transmitted,
                "deadLetter", dead
        );
    }
}
