package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.InsuranceClaim;
import com.example.pos.insurance.model.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    Optional<InsuranceClaim> findByClaimReference(String claimReference);

    List<InsuranceClaim> findBySaleId(Long saleId);

    List<InsuranceClaim> findByInsurerId(Long insurerId);

    List<InsuranceClaim> findByClaimStatus(ClaimStatus status);

    List<InsuranceClaim> findByInsurerIdAndClaimStatus(Long insurerId, ClaimStatus status);

    List<InsuranceClaim> findByBatchReference(String batchReference);

    @Query("SELECT c FROM InsuranceClaim c WHERE c.claimStatus IN (" +
            "com.example.pos.insurance.model.ClaimStatus.PENDING, " +
            "com.example.pos.insurance.model.ClaimStatus.PREAUTH_OBTAINED)")
    List<InsuranceClaim> findPendingForSubmission();

    @Query("SELECT c FROM InsuranceClaim c WHERE c.claimStatus = " +
            "com.example.pos.insurance.model.ClaimStatus.SUBMITTED")
    List<InsuranceClaim> findSubmittedUnsettled();
}
