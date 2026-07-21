package com.example.pos.compliance.controlledrugs.repository;

import com.example.pos.compliance.controlledrugs.model.ControlledDrugs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlledDrugsRepository extends JpaRepository<ControlledDrugs, Long> {

    List<ControlledDrugs> findByMedicineId(Long medicineId);
}
