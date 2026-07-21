package com.example.pos.compliance.transmission.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.model.InvoiceHistoryType;
import com.example.pos.compliance.invoice.service.InvoiceService;
import com.example.pos.compliance.transmission.model.Transmission;
import com.example.pos.compliance.transmission.model.TransmissionAttempt;
import com.example.pos.compliance.transmission.model.TransmissionStatus;
import com.example.pos.compliance.transmission.repository.TransmissionAttemptRepository;
import com.example.pos.compliance.transmission.repository.TransmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class TransmissionService {

    private static final Logger log = LoggerFactory.getLogger(TransmissionService.class);

    private final TransmissionRepository transmissionRepo;
    private final TransmissionAttemptRepository attemptRepo;
    private final InvoiceService invoiceService;
    private final InMemoryTransmissionQueue queue;

    public TransmissionService(TransmissionRepository transmissionRepo,
                               TransmissionAttemptRepository attemptRepo,
                               InvoiceService invoiceService,
                               InMemoryTransmissionQueue queue) {
        this.transmissionRepo = transmissionRepo;
        this.attemptRepo = attemptRepo;
        this.invoiceService = invoiceService;
        this.queue = queue;
    }

    public Transmission createAndQueue(Long invoiceId, String documentType, Long submittedBy) {
        Transmission tx = Transmission.builder()
                .invoiceId(invoiceId)
                .documentType(documentType)
                .transmissionStatus(TransmissionStatus.PENDING)
                .submittedBy(submittedBy)
                .submittedAt(LocalDateTime.now())
                .idempotencyKey(UUID.randomUUID().toString().replace("-", ""))
                .payloadVersion(1)
                .build();

        tx = transmissionRepo.save(tx);
        queue.enqueue(tx.getId());
        return tx;
    }

    @Transactional(readOnly = true)
    public Transmission getById(Long id) {
        return transmissionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transmission", id));
    }

    @Transactional(readOnly = true)
    public Transmission getByInvoiceId(Long invoiceId) {
        return transmissionRepo.findByInvoiceId(invoiceId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Transmission getByIdempotencyKey(String idempotencyKey) {
        return transmissionRepo.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    public Transmission markTransmitting(Long id, String kraRequest) {
        Transmission tx = getById(id);
        tx.setTransmissionStatus(TransmissionStatus.TRANSMITTING);
        tx.setKraRequest(kraRequest);
        tx.setRequestHash(sha256(kraRequest));

        TransmissionAttempt attempt = TransmissionAttempt.builder()
                .transmission(tx)
                .attemptNumber(tx.getAttempts() != null ? tx.getAttempts().size() + 1 : 1)
                .sentAt(LocalDateTime.now())
                .requestPayload(kraRequest)
                .build();
        attemptRepo.save(attempt);

        invoiceService.recordTransmissionSent(tx.getInvoiceId(),
                "Attempt " + attempt.getAttemptNumber(), tx.getSubmittedBy(), null);

        return transmissionRepo.save(tx);
    }

    public Transmission markTransmitted(Long id, String kraResponse, String kraReceiptNumber,
                                         Long durationMs) {
        Transmission tx = getById(id);
        tx.setTransmissionStatus(TransmissionStatus.TRANSMITTED);
        tx.setKraResponse(kraResponse);
        tx.setKraReceiptNumber(kraReceiptNumber);
        tx.setResponseHash(sha256(kraResponse));

        TransmissionAttempt lastAttempt = attemptRepo
                .findByTransmissionIdOrderByAttemptNumberDesc(id).stream().findFirst().orElse(null);
        if (lastAttempt != null) {
            lastAttempt.setSuccess(true);
            lastAttempt.setResponseAt(LocalDateTime.now());
            lastAttempt.setResponsePayload(kraResponse);
            lastAttempt.setDurationMs(durationMs);
            attemptRepo.save(lastAttempt);
        }

        invoiceService.recordTransmissionAcknowledged(tx.getInvoiceId(), kraReceiptNumber,
                tx.getSubmittedBy(), null);

        return transmissionRepo.save(tx);
    }

    public Transmission markFailed(Long id, String failureReason, String kraResponse, int statusCode) {
        Transmission tx = getById(id);
        tx.setTransmissionStatus(TransmissionStatus.FAILED);
        tx.setFailureReason(failureReason);
        tx.setKraResponse(kraResponse);
        tx.setResponseHash(kraResponse != null ? sha256(kraResponse) : null);
        tx.setNextRetryTime(calculateNextRetry(tx.getAttempts() != null ? tx.getAttempts().size() : 1));

        TransmissionAttempt lastAttempt = attemptRepo
                .findByTransmissionIdOrderByAttemptNumberDesc(id).stream().findFirst().orElse(null);
        if (lastAttempt != null) {
            lastAttempt.setSuccess(false);
            lastAttempt.setStatusCode(statusCode);
            lastAttempt.setErrorMessage(failureReason);
            lastAttempt.setResponsePayload(kraResponse);
            lastAttempt.setResponseAt(LocalDateTime.now());
            attemptRepo.save(lastAttempt);
        }

        invoiceService.recordTransmissionFailed(tx.getInvoiceId(), failureReason,
                tx.getSubmittedBy(), null);

        return transmissionRepo.save(tx);
    }

    public void requeueFailed() {
        var failed = transmissionRepo.findByTransmissionStatus(TransmissionStatus.FAILED);
        for (Transmission tx : failed) {
            if (tx.getNextRetryTime() != null && tx.getNextRetryTime().isBefore(LocalDateTime.now())) {
                queue.enqueue(tx.getId());
            }
        }
    }

    private LocalDateTime calculateNextRetry(int attemptCount) {
        long delayMinutes = (long) Math.min(Math.pow(2, attemptCount), 1440);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    private String sha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
