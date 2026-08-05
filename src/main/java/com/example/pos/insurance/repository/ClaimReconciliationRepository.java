package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.ClaimReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimReconciliationRepository extends JpaRepository<ClaimReconciliation, UUID> {
    List<ClaimReconciliation> findByInsurerId(UUID insurerId);
    Optional<ClaimReconciliation> findByInsurerIdAndPeriodStartAndPeriodEnd(
            UUID insurerId, java.time.LocalDate start, java.time.LocalDate end);
}
