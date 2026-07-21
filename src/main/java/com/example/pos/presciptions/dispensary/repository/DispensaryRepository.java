package com.example.pos.presciptions.dispensary.repository;

import com.example.pos.presciptions.dispensary.model.Dispensary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispensaryRepository extends JpaRepository<Dispensary, Long> {

    List<Dispensary> findByUserId(Long userId);
    List<Dispensary> findByMedicineBatchesId(Long batchId);
}
