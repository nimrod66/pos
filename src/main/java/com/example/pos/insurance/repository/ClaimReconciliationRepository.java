package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.ClaimReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimReconciliationRepository extends JpaRepository<ClaimReconciliation, Long> {
    List<ClaimReconciliation> findByInsurerId(Long insurerId);
    Optional<ClaimReconciliation> findByInsurerIdAndPeriodStartAndPeriodEnd(
            Long insurerId, java.time.LocalDate start, java.time.LocalDate end);
}
