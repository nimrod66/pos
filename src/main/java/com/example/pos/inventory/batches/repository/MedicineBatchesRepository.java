package com.example.pos.inventory.batches.repository;

import java.util.UUID;

import com.example.pos.inventory.batches.model.MedicineBatches;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicineBatchesRepository extends JpaRepository<MedicineBatches, UUID> {

    @EntityGraph(attributePaths = {"medicine", "medicine.pharmacy"})
    Optional<MedicineBatches> findByIdAndMedicinePharmacyId(UUID id, UUID pharmacyId);

    @EntityGraph(attributePaths = {"medicine", "medicine.pharmacy"})
    Page<MedicineBatches> findByMedicinePharmacyId(UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"medicine", "medicine.pharmacy"})
    Page<MedicineBatches> findByMedicineIdAndMedicinePharmacyId(
            UUID medicineId, UUID pharmacyId, Pageable pageable);

    @EntityGraph(attributePaths = {"medicine", "medicine.pharmacy"})
    Page<MedicineBatches> findByMedicinePharmacyIdAndExpirationDateBefore(
            UUID pharmacyId, LocalDate date, Pageable pageable);

    @EntityGraph(attributePaths = {"medicine", "medicine.pharmacy"})
    List<MedicineBatches> findByMedicineIdAndMedicinePharmacyIdAndExpirationDateAfter(
            UUID medicineId, UUID pharmacyId, LocalDate date);

    boolean existsByMedicineIdAndBatchNumberIgnoreCase(UUID medicineId, String batchNumber);

    boolean existsByMedicineIdAndBatchNumberIgnoreCaseAndIdNot(
            UUID medicineId, String batchNumber, UUID id);

    Optional<MedicineBatches> findByBatchNumber(String batchNumber);

    Optional<MedicineBatches> findByBatchNumberAndMedicineId(String batchNumber, UUID medicineId);

    List<MedicineBatches> findByMedicineId(UUID medicineId);

    Page<MedicineBatches> findByMedicineId(UUID medicineId, Pageable pageable);

    boolean existsByBatchNumber(String batchNumber);

    List<MedicineBatches> findByExpirationDateBefore(LocalDate date);

    Page<MedicineBatches> findByExpirationDateBefore(LocalDate date, Pageable pageable);

    List<MedicineBatches> findByExpirationDateBetween(LocalDate start, LocalDate end);

    List<MedicineBatches> findByMedicineIdAndExpirationDateAfter(UUID medicineId, LocalDate date);
}
