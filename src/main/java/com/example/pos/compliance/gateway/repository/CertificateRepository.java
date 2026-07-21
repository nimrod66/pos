package com.example.pos.compliance.gateway.repository;

import com.example.pos.compliance.gateway.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findBySerial(String serial);

    List<Certificate> findByTenantIdAndStatus(Long tenantId, Certificate.CertificateStatus status);
}
