package com.example.pos.compliance.batch.repository;

import com.example.pos.compliance.batch.model.Batch;
import com.example.pos.compliance.batch.model.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByBatchStatus(BatchStatus status);
}
