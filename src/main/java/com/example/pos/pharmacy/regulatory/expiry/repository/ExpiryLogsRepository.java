package com.example.pos.pharmacy.regulatory.expiry.repository;

import com.example.pos.pharmacy.regulatory.expiry.model.ExpiryLogs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpiryLogsRepository extends JpaRepository<ExpiryLogs, Long> {

    List<ExpiryLogs> findByMedicineBatchesId(Long batchId);
}
