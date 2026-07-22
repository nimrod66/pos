package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.InsuranceScheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceSchemeRepository extends JpaRepository<InsuranceScheme, Long> {
    List<InsuranceScheme> findByInsurerId(Long insurerId);
    List<InsuranceScheme> findByInsurerIdAndStatus(Long insurerId, InsuranceScheme.SchemeStatus status);
    boolean existsByCode(String code);
}
