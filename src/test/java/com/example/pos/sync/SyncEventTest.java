package com.example.pos.sync;

import com.example.pos.sync.event.EventType;
import com.example.pos.sync.event.SyncEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SyncEventTest {

    @Test
    void shouldAutoGenerateEventId() {
        SyncEvent event = SyncEvent.builder()
                .aggregateType("SALE")
                .aggregateId("uuid-001")
                .eventType(EventType.SALE_CREATED)
                .payload("{}")
                .terminalId("TERM-001")
                .build();

        event.prePersist();

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).hasSize(36);
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(SyncEvent.SyncEventStatus.PENDING);
    }

    @Test
    void shouldCalculateExponentialBackoff() {
        SyncEvent event = SyncEvent.builder()
                .aggregateType("SALE")
                .aggregateId("uuid-001")
                .eventType(EventType.SALE_CREATED)
                .payload("{}")
                .terminalId("TERM-001")
                .build();

        event.prePersist();
        event.markFailed("Network timeout");
        assertThat(event.getStatus()).isEqualTo(SyncEvent.SyncEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isAfter(LocalDateTime.now());

        for (int i = 0; i < 8; i++) {
            event.markFailed("Retry " + (i + 2));
        }

        assertThat(event.getRetryCount()).isEqualTo(9);
    }

    @Test
    void shouldMarkDeadAfterMaxRetries() {
        SyncEvent event = SyncEvent.builder()
                .aggregateType("SALE")
                .aggregateId("uuid-001")
                .eventType(EventType.SALE_CREATED)
                .payload("{}")
                .terminalId("TERM-001")
                .build();

        event.prePersist();
        event.markDead("Max retries exceeded");

        assertThat(event.getStatus()).isEqualTo(SyncEvent.SyncEventStatus.DEAD);
        assertThat(event.getLastError()).isEqualTo("Max retries exceeded");
    }

    @Test
    void shouldTrackAcknowledgedTimestamp() {
        SyncEvent event = SyncEvent.builder()
                .aggregateType("SALE")
                .aggregateId("uuid-001")
                .eventType(EventType.SALE_CREATED)
                .payload("{}")
                .terminalId("TERM-001")
                .build();

        event.prePersist();
        event.markAcknowledged();

        assertThat(event.getStatus()).isEqualTo(SyncEvent.SyncEventStatus.SENT);
        assertThat(event.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void defaultVersionsAreOne() {
        SyncEvent event = SyncEvent.builder()
                .aggregateType("SALE")
                .aggregateId("uuid-001")
                .eventType(EventType.SALE_CREATED)
                .payload("{}")
                .terminalId("TERM-001")
                .build();

        assertThat(event.getEventVersion()).isEqualTo(1);
        assertThat(event.getAggregateVersion()).isEqualTo(1);
        assertThat(event.getSequenceNumber()).isEqualTo(1);
    }
}
