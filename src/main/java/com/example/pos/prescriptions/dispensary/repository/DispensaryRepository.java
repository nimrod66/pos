package com.example.pos.prescriptions.dispensary.repository;

import java.util.UUID;

import com.example.pos.prescriptions.dispensary.model.Dispensary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispensaryRepository extends JpaRepository<Dispensary, UUID> {

    List<Dispensary> findByUserId(UUID userId);
    List<Dispensary> findByMedicineBatchesId(UUID batchId);
}
