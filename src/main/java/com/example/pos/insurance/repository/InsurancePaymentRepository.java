package com.example.pos.insurance.repository;

import com.example.pos.insurance.model.InsurancePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurancePaymentRepository extends JpaRepository<InsurancePayment, Long> {
    Optional<InsurancePayment> findByPaymentReference(String reference);
    List<InsurancePayment> findByInsurerId(Long insurerId);
}
