package com.example.pos.compliance.event.repository;

import java.util.UUID;

import com.example.pos.compliance.event.model.ComplianceEvent;
import com.example.pos.compliance.event.model.ComplianceEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, UUID> {

    List<ComplianceEvent> findByInvoiceIdOrderByCreatedAtAsc(UUID invoiceId);

    List<ComplianceEvent> findByEventTypeAndCreatedAtAfter(ComplianceEventType type, java.time.LocalDateTime since);

    long countByEventTypeAndCreatedAtAfter(ComplianceEventType type, java.time.LocalDateTime since);
}
