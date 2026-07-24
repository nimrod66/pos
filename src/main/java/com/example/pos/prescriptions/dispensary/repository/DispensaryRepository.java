package com.example.pos.prescriptions.dispensary.repository;

import com.example.pos.prescriptions.dispensary.model.Dispensary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispensaryRepository extends JpaRepository<Dispensary, Long> {

    List<Dispensary> findByUserId(Long userId);
    List<Dispensary> findByMedicineBatchesId(Long batchId);
}
