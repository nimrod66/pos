package com.example.pos.compliance.transmission.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "transmissions")
public class Transmission extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_status", nullable = false, length = 20)
    @Builder.Default
    private TransmissionStatus transmissionStatus = TransmissionStatus.PENDING;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response_hash", length = 64)
    private String responseHash;

    @Column(name = "payload_version")
    @Builder.Default
    private Integer payloadVersion = 1;

    @Column(name = "kra_request", columnDefinition = "LONGTEXT")
    private String kraRequest;

    @Column(name = "kra_response", columnDefinition = "LONGTEXT")
    private String kraResponse;

    @Column(name = "kra_receipt_number", length = 100)
    private String kraReceiptNumber;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Builder.Default
    @OneToMany(mappedBy = "transmission", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TransmissionAttempt> attempts = new ArrayList<>();
}
