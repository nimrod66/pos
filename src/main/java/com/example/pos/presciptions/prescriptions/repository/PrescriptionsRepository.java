package com.example.pos.presciptions.prescriptions.repository;

import com.example.pos.presciptions.prescriptions.model.Prescriptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionsRepository extends JpaRepository<Prescriptions, Long> {

    Optional<Prescriptions> findByPrescriptionNumber(String number);
}
