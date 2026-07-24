package com.example.pos.prescriptions.prescriptionitems.repository;

import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemsRepository extends JpaRepository<PrescriptionItems, Long> {
}
