package com.example.pos.sync;

import com.example.pos.sync.event.*;
import com.example.pos.sync.service.ConnectivityService;
import com.example.pos.sync.service.SyncService;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.config.SyncProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private SyncEventRepository outboxRepo;

    @Mock
    private ConnectivityService connectivity;

    @Mock
    private TerminalConfig terminalConfig;

    @Mock
    private SyncProperties syncProperties;

    @Test
    void shouldWriteOutboxEventWithCorrectVersioning() {
        when(syncProperties.isEnabled()).thenReturn(true);
        when(terminalConfig.getTerminalId()).thenReturn("TERM-TEST01");
        when(outboxRepo.countByAggregateTypeAndAggregateId("SALE", "uuid-123")).thenReturn(3L);
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        svc.writeOutboxEvent(EventType.SALE_CREATED, "SALE", "uuid-123", "{}");

        ArgumentCaptor<SyncEvent> event = ArgumentCaptor.forClass(SyncEvent.class);
        verify(outboxRepo).save(event.capture());
        assertThat(event.getValue().getAggregateVersion()).isEqualTo(4);
        assertThat(event.getValue().getSequenceNumber()).isEqualTo(4);
        assertThat(event.getValue().getTerminalId()).isEqualTo("TERM-TEST01");
    }

    @Test
    void shouldAcceptTerminalWinsEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> incoming = Map.of(
                "eventId", "evt-001", "aggregateType", "SALE", "aggregateId", "sale-uuid-001",
                "eventType", "SALE_CREATED", "payload", "{}", "terminalId", "TERM-A",
                "aggregateVersion", 1, "sequenceNumber", 1
        );
        Map<String, Object> result = svc.receivePush(incoming);
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
    }

    @Test
    void shouldRejectCentralWinsEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        Map<String, Object> incoming = Map.of(
                "eventId", "evt-002", "aggregateType", "PRODUCT", "aggregateId", "prod-001",
                "eventType", "PRODUCT_UPDATED", "payload", "{}", "terminalId", "TERM-A"
        );
        Map<String, Object> result = svc.receivePush(incoming);
        assertThat(result.get("status")).isEqualTo("CONFLICT");
    }

    @Test
    void shouldAcceptStockMovementEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> incoming = Map.of(
                "eventId", "evt-003", "aggregateType", "STOCK", "aggregateId", "stock-001",
                "eventType", "STOCK_DEDUCTED", "payload", "{}", "terminalId", "TERM-A",
                "aggregateVersion", 1
        );
        Map<String, Object> result = svc.receivePush(incoming);
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        assertThat(result.get("message")).asString().contains("Stock movement");
    }

    @Test
    void shouldIgnoreDuplicateEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        when(outboxRepo.existsById("evt-001")).thenReturn(true);

        Map<String, Object> incoming = Map.of(
                "eventId", "evt-001", "aggregateType", "SALE", "aggregateId", "sale-uuid-001",
                "eventType", "SALE_CREATED", "payload", "{}", "terminalId", "TERM-A"
        );
        Map<String, Object> result = svc.receivePush(incoming);
        assertThat(result.get("status")).isEqualTo("IDEMPOTENT_IGNORE");
    }

    @Test
    void shouldVersionCheckCustomerEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        when(outboxRepo.findByAggregateTypeAndAggregateIdOrderByAggregateVersionDesc("CUSTOMER", "cust-001"))
                .thenReturn(List.of());
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> incoming = Map.of(
                "eventId", "evt-004", "aggregateType", "CUSTOMER", "aggregateId", "cust-001",
                "eventType", "CUSTOMER_UPDATED", "payload", "{}", "terminalId", "TERM-A",
                "aggregateVersion", 5
        );
        Map<String, Object> result = svc.receivePush(incoming);
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
    }

    @Test
    void shouldRejectStaleVersionedEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        SyncEvent existing = SyncEvent.builder()
                .eventId("existing-001").aggregateType("CUSTOMER").aggregateId("cust-001")
                .eventType(EventType.CUSTOMER_UPDATED).payload("{}").terminalId("TERM-B")
                .aggregateVersion(10).status(SyncEvent.SyncEventStatus.SENT).build();
        when(outboxRepo.findByAggregateTypeAndAggregateIdOrderByAggregateVersionDesc("CUSTOMER", "cust-001"))
                .thenReturn(List.of(existing));

        Map<String, Object> incoming = Map.of(
                "eventId", "evt-005", "aggregateType", "CUSTOMER", "aggregateId", "cust-001",
                "eventType", "CUSTOMER_UPDATED", "payload", "{}", "terminalId", "TERM-A",
                "aggregateVersion", 5
        );
        Map<String, Object> result = svc.receivePush(incoming);
        assertThat(result.get("status")).isEqualTo("IDEMPOTENT_IGNORE");
    }

    @Test
    void shouldRetryDeadEvents() {
        SyncService svc = new SyncService(outboxRepo, connectivity, terminalConfig, syncProperties);
        SyncEvent dead = SyncEvent.builder()
                .eventId("dead-001").aggregateType("SALE").aggregateId("sale-001")
                .eventType(EventType.SALE_CREATED).payload("{}").terminalId("TERM-A")
                .status(SyncEvent.SyncEventStatus.DEAD).retryCount(10)
                .lastError("Max retries exceeded").aggregateVersion(3).build();
        when(outboxRepo.findById("dead-001")).thenReturn(Optional.of(dead));
        when(outboxRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncEvent result = svc.retryDeadEvent("dead-001");
        assertThat(result.getStatus()).isEqualTo(SyncEvent.SyncEventStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getAggregateVersion()).isEqualTo(4);
    }
}
