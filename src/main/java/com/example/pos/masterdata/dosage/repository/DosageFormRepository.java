package com.example.pos.masterdata.dosage.repository;

import java.util.UUID;

import com.example.pos.masterdata.dosage.model.DosageForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DosageFormRepository extends JpaRepository<DosageForm, UUID> {

    Optional<DosageForm> findByFormName(String formName);

    boolean existsByFormName(String formName);

    boolean existsByFormNameAndIdNot(String formName, UUID id);
}
