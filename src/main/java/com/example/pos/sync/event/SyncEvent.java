package com.example.pos.sync.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "sync_outbox", indexes = {
        @Index(name = "idx_sync_outbox_status", columnList = "status"),
        @Index(name = "idx_sync_outbox_retry", columnList = "status,nextRetryAt"),
        @Index(name = "idx_sync_outbox_aggregate", columnList = "aggregateType,aggregateId")
})
public class SyncEvent {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false, length = 36)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType eventType;

    @Builder.Default
    private int eventVersion = 1;

    @Builder.Default
    private int aggregateVersion = 1;

    @Builder.Default
    private int sequenceNumber = 1;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SyncEventStatus status = SyncEventStatus.PENDING;

    @Builder.Default
    private int retryCount = 0;

    private LocalDateTime nextRetryAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private LocalDateTime acknowledgedAt;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false, length = 36)
    private String terminalId;

    @PrePersist
    public void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == SyncEventStatus.PENDING && nextRetryAt == null) {
            nextRetryAt = createdAt;
        }
    }

    public enum SyncEventStatus {
        PENDING, SENDING, SENT, FAILED, DEAD, IGNORED
    }

    public void markSent() {
        this.status = SyncEventStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAcknowledged() {
        this.status = SyncEventStatus.SENT;
        this.acknowledgedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.retryCount++;
        this.status = SyncEventStatus.FAILED;
        this.lastError = error;
        this.nextRetryAt = calculateNextRetry();
    }

    public void markDead(String error) {
        this.status = SyncEventStatus.DEAD;
        this.lastError = error;
    }

    private LocalDateTime calculateNextRetry() {
        int[] delays = {15, 30, 60, 120, 300, 600, 1800};
        int index = Math.min(retryCount - 1, delays.length - 1);
        return LocalDateTime.now().plusSeconds(delays[Math.max(0, index)]);
    }
}
