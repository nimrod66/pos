package com.example.pos.compliance.batch.repository;

import com.example.pos.compliance.batch.model.BatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchItemRepository extends JpaRepository<BatchItem, Long> {

    List<BatchItem> findByBatchId(Long batchId);
}
