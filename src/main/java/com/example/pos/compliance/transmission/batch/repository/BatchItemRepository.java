package com.example.pos.compliance.transmission.batch.repository;

import java.util.UUID;

import com.example.pos.compliance.transmission.batch.model.BatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchItemRepository extends JpaRepository<BatchItem, UUID> {

    List<BatchItem> findByBatchId(UUID batchId);
}
