package com.example.pos.compliance.event.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "compliance_events", indexes = {
        @Index(name = "idx_ce_invoice", columnList = "invoice_id"),
        @Index(name = "idx_ce_type", columnList = "event_type"),
        @Index(name = "idx_ce_created", columnList = "created_at")
})
public class ComplianceEvent extends BaseEntity {

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ComplianceEventType eventType;

    @Column(length = 2000)
    private String description;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "terminal_id", length = 36)
    private String terminalId;

    @Column(name = "terminal_name", length = 100)
    private String terminalName;
}
