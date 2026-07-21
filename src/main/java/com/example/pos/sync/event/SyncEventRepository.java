package com.example.pos.sync.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncEventRepository extends JpaRepository<SyncEvent, String> {

    List<SyncEvent> findByStatusOrderByCreatedAtAsc(SyncEvent.SyncEventStatus status);

    List<SyncEvent> findByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
            SyncEvent.SyncEventStatus status, java.time.LocalDateTime now);

    List<SyncEvent> findByTerminalId(String terminalId);

    long countByStatus(SyncEvent.SyncEventStatus status);

    long countByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    List<SyncEvent> findByAggregateTypeAndAggregateIdOrderByAggregateVersionDesc(
            String aggregateType, String aggregateId);

    List<SyncEvent> findByTerminalIdAndStatus(String terminalId, SyncEvent.SyncEventStatus status);

    long countByTerminalIdAndStatus(String terminalId, SyncEvent.SyncEventStatus status);
}
