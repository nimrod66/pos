package com.example.pos.compliance.gateway.repository;

import java.util.UUID;

import com.example.pos.compliance.gateway.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findBySerial(String serial);

    List<Certificate> findByTenantIdAndStatus(UUID tenantId, Certificate.CertificateStatus status);
}
