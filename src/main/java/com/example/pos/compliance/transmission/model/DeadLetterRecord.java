package com.example.pos.compliance.transmission.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "dead_letter_records")
public class DeadLetterRecord extends BaseEntity {

    @Column(name = "transmission_id", nullable = false)
    private Long transmissionId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "attempts_exhausted")
    private int attemptsExhausted;

    @Column(name = "failure_reason", length = 4000)
    private String failureReason;

    @Column(name = "last_kra_response", columnDefinition = "TEXT")
    private String lastKraResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeadLetterStatus status = DeadLetterStatus.PENDING;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "resolution", length = 2000)
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "tenant_id")
    private Long tenantId;

    public enum DeadLetterStatus {
        PENDING, IN_REVIEW, RETRYING, RESOLVED, DISCARDED
    }
}
