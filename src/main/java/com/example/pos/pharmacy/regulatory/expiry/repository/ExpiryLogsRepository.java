package com.example.pos.pharmacy.regulatory.expiry.repository;

import java.util.UUID;

import com.example.pos.pharmacy.regulatory.expiry.model.ExpiryLogs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpiryLogsRepository extends JpaRepository<ExpiryLogs, UUID> {

    List<ExpiryLogs> findByMedicineBatchesId(UUID batchId);
}
