package com.example.pos.presciptions.prescriptionitems.repository;

import com.example.pos.presciptions.prescriptionitems.model.PrescriptionItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemsRepository extends JpaRepository<PrescriptionItems, Long> {
}
