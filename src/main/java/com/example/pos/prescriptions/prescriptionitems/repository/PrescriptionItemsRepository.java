package com.example.pos.prescriptions.prescriptionitems.repository;

import java.util.UUID;

import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemsRepository extends JpaRepository<PrescriptionItems, UUID> {
}
