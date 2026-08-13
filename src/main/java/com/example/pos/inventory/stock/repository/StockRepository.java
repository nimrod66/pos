package com.example.pos.inventory.stock.repository;

import java.util.UUID;

import com.example.pos.inventory.stock.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    List<Stock> findByBranchId(UUID branchId);

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    List<Stock> findByBranchIdIn(List<UUID> branchIds);

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    Page<Stock> findByBranchId(UUID branchId, Pageable pageable);

    Optional<Stock> findByMedicineBatchesId(UUID batchId);

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    Optional<Stock> findByBranchIdAndMedicineBatchesId(UUID branchId, UUID batchId);

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    Optional<Stock> findDetailedByIdAndBranchId(UUID id, UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.branch.id = :branchId and s.medicineBatches.id = :batchId")
    Optional<Stock> findForUpdate(@Param("branchId") UUID branchId, @Param("batchId") UUID batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s join fetch s.medicineBatches batch join fetch batch.medicine medicine "
            + "where s.branch.id = :branchId and medicine.id = :medicineId "
            + "and medicine.status = com.example.pos.masterdata.medicine.model.Medicine.Status.AVAILABLE "
            + "and s.quantityAvailable > 0 and (batch.expirationDate is null or batch.expirationDate > :today) "
            + "order by case when batch.expirationDate is null then 1 else 0 end, batch.expirationDate, batch.createdAt")
    List<Stock> findSellableFefoForUpdate(@Param("branchId") UUID branchId,
                                           @Param("medicineId") UUID medicineId,
                                           @Param("today") LocalDate today);

    List<Stock> findByBranchIdAndQuantityAvailableLessThanEqual(UUID branchId, Integer threshold);

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    List<Stock> findByBranchIdAndMedicineBatches_Medicine_Id(UUID branchId, UUID medicineId);

    @EntityGraph(attributePaths = {"branch", "medicineBatches", "medicineBatches.medicine"})
    List<Stock> findByBranchIdAndQuantityAvailableGreaterThan(UUID branchId, int minQuantity);
}
