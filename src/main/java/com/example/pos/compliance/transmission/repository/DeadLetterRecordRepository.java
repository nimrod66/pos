package com.example.pos.compliance.transmission.repository;

import java.util.UUID;

import com.example.pos.compliance.transmission.model.DeadLetterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeadLetterRecordRepository extends JpaRepository<DeadLetterRecord, UUID> {

    List<DeadLetterRecord> findByStatus(DeadLetterRecord.DeadLetterStatus status);

    long countByStatus(DeadLetterRecord.DeadLetterStatus status);
}
