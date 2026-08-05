package com.example.pos.pharmacy.regulatory.controlledrugs.repository;

import java.util.UUID;

import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlledDrugsRepository extends JpaRepository<ControlledDrugs, UUID> {

    List<ControlledDrugs> findByMedicineId(UUID medicineId);
}
