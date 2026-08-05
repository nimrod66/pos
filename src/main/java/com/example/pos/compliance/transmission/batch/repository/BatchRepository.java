package com.example.pos.compliance.transmission.batch.repository;

import java.util.UUID;

import com.example.pos.compliance.transmission.batch.model.Batch;
import com.example.pos.compliance.transmission.batch.model.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    List<Batch> findByBatchStatus(BatchStatus status);
}
