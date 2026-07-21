package com.example.pos.compliance.expiry.repository;

import com.example.pos.compliance.expiry.model.ExpiryLogs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpiryLogsRepository extends JpaRepository<ExpiryLogs, Long> {

    List<ExpiryLogs> findByMedicineBatchesId(Long batchId);
}
