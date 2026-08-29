package com.example.pos.operations.repository;

import com.example.pos.operations.model.OperationalMetricEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationalMetricEventRepository extends JpaRepository<OperationalMetricEvent, UUID> {

    long countByEventTypeAndStatusAndCreatedAtBetween(
            OperationalMetricEvent.EventType eventType,
            OperationalMetricEvent.EventStatus status,
            LocalDateTime from,
            LocalDateTime to);

    long countByEventTypeAndCreatedAtBetween(
            OperationalMetricEvent.EventType eventType,
            LocalDateTime from,
            LocalDateTime to);

    @Query("""
            select e.eventType, e.status, count(e)
            from OperationalMetricEvent e
            where e.createdAt between :from and :to
              and (:branchId is null or e.branch.id = :branchId)
            group by e.eventType, e.status
            """)
    List<Object[]> countByTypeAndStatus(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("branchId") UUID branchId);

    @Query("""
            select e.reasonCode, count(e)
            from OperationalMetricEvent e
            where e.eventType = :eventType
              and e.status = :status
              and e.createdAt between :from and :to
              and (:branchId is null or e.branch.id = :branchId)
            group by e.reasonCode
            order by count(e) desc
            """)
    List<Object[]> countReasons(@Param("eventType") OperationalMetricEvent.EventType eventType,
                                @Param("status") OperationalMetricEvent.EventStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                @Param("branchId") UUID branchId);
}
