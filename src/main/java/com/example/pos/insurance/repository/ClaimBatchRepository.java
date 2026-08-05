package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.ClaimBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimBatchRepository extends JpaRepository<ClaimBatch, UUID> {
    Optional<ClaimBatch> findByBatchReference(String reference);
    List<ClaimBatch> findByInsurerId(UUID insurerId);
    List<ClaimBatch> findByStatus(ClaimBatch.BatchStatus status);
    List<ClaimBatch> findByInsurerIdAndStatus(UUID insurerId, ClaimBatch.BatchStatus status);
}
