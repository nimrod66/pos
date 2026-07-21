package com.example.pos.compliance.reconciliation.service;

import com.example.pos.compliance.reconciliation.model.ReconciliationResult;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final TransmissionRepository transmissionRepo;

    public ReconciliationService(TransmissionRepository transmissionRepo) {
        this.transmissionRepo = transmissionRepo;
    }

    public ReconciliationResult runReconciliation() {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting compliance reconciliation");

        long transmittedCount = transmissionRepo.countByTransmissionStatus(TransmissionStatus.TRANSMITTED);
        long failedCount = transmissionRepo.countByTransmissionStatus(TransmissionStatus.FAILED);
        long pendingCount = transmissionRepo.countByTransmissionStatus(TransmissionStatus.PENDING);

        return ReconciliationResult.builder()
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .localInvoiceCount(transmittedCount + pendingCount + failedCount)
                .kraAcknowledgedCount(transmittedCount)
                .missingAtKra(pendingCount + failedCount)
                .orphanedAtKra(0)
                .success(true)
                .build();
    }
}
