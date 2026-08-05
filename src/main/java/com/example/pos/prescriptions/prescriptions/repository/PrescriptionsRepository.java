package com.example.pos.prescriptions.prescriptions.repository;

import java.util.UUID;

import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionsRepository extends JpaRepository<Prescriptions, UUID> {

    Optional<Prescriptions> findByPrescriptionNumber(String number);
}
