package com.example.pos.masterdata.dosage.repository;

import com.example.pos.masterdata.dosage.model.DosageForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DosageFormRepository extends JpaRepository<DosageForm, Long> {

    Optional<DosageForm> findByFormName(String formName);

    boolean existsByFormName(String formName);

    boolean existsByFormNameAndIdNot(String formName, Long id);
}
