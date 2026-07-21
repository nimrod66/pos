package com.example.pos.compliance.transmission.repository;

import com.example.pos.compliance.transmission.model.DeadLetterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeadLetterRecordRepository extends JpaRepository<DeadLetterRecord, Long> {

    List<DeadLetterRecord> findByStatus(DeadLetterRecord.DeadLetterStatus status);

    long countByStatus(DeadLetterRecord.DeadLetterStatus status);
}
