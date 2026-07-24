package com.example.pos.inventory.batches.repository;

import com.example.pos.inventory.batches.model.MedicineBatches;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicineBatchesRepository extends JpaRepository<MedicineBatches, Long> {

    Optional<MedicineBatches> findByBatchNumber(String batchNumber);

    List<MedicineBatches> findByMedicineId(Long medicineId);

    Page<MedicineBatches> findByMedicineId(Long medicineId, Pageable pageable);

    boolean existsByBatchNumber(String batchNumber);

    List<MedicineBatches> findByExpirationDateBefore(LocalDate date);

    Page<MedicineBatches> findByExpirationDateBefore(LocalDate date, Pageable pageable);

    List<MedicineBatches> findByExpirationDateBetween(LocalDate start, LocalDate end);

    List<MedicineBatches> findByMedicineIdAndExpirationDateAfter(Long medicineId, LocalDate date);
}
