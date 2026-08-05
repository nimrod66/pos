package com.example.pos.core.pharmacy.repository;

import java.util.UUID;

import com.example.pos.core.pharmacy.model.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PharmacyRepository extends JpaRepository<Pharmacy, UUID> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

    Optional<Pharmacy> findByEmail(String email);
}
