package com.example.pos.insurance.repository;

import java.util.UUID;

import com.example.pos.insurance.model.InsuranceScheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceSchemeRepository extends JpaRepository<InsuranceScheme, UUID> {
    List<InsuranceScheme> findByInsurerId(UUID insurerId);
    List<InsuranceScheme> findByInsurerIdAndStatus(UUID insurerId, InsuranceScheme.SchemeStatus status);
    boolean existsByCode(String code);
}
