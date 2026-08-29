package com.example.pos.operations.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "operational_metric_events", indexes = {
        @Index(name = "idx_operational_metric_created", columnList = "created_at"),
        @Index(name = "idx_operational_metric_type_status", columnList = "event_type,status"),
        @Index(name = "idx_operational_metric_branch_created", columnList = "branch_id,created_at")
})
public class OperationalMetricEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EventStatus status;

    @Column(name = "reason_code", length = 96)
    private String reasonCode;

    @Column(name = "source", length = 96)
    private String source;

    @Column(name = "terminal_id", length = 96)
    private String terminalId;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    public enum EventType {
        CHECKOUT,
        PAYMENT,
        BACKUP,
        RESTORE,
        OFFLINE_QUEUE,
        HARDWARE,
        LOGIN,
        PERMISSION_DENIED,
        SYNC,
        INSTALLER,
        PILOT_VALIDATION
    }

    public enum EventStatus {
        ATTEMPTED,
        SUCCESS,
        FAILED,
        WARNING,
        STALE,
        PENDING
    }
}
