package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.InsuranceClaim;
import com.example.pos.insurance.model.ClaimStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, UUID> {

    @Override
    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findAll();

    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    Optional<InsuranceClaim> findByClaimReference(String claimReference);

    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findBySaleId(UUID saleId);

    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findByInsurerId(UUID insurerId);

    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findByClaimStatus(ClaimStatus status);

    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findByInsurerIdAndClaimStatus(UUID insurerId, ClaimStatus status);

    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findByBatch_BatchReference(String batchReference);

    @Query("SELECT c FROM InsuranceClaim c WHERE c.claimStatus IN (" +
            "com.example.pos.insurance.model.ClaimStatus.PENDING, " +
            "com.example.pos.insurance.model.ClaimStatus.PREAUTH_OBTAINED)")
    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findPendingForSubmission();

    @Query("SELECT c FROM InsuranceClaim c WHERE c.claimStatus = " +
            "com.example.pos.insurance.model.ClaimStatus.SUBMITTED")
    @EntityGraph(attributePaths = {"insurer", "scheme", "member", "authorization", "batch", "payment"})
    List<InsuranceClaim> findSubmittedUnsettled();
}
