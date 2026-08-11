package com.example.pos.inventory.stockmovements.repository;

import java.util.UUID;

import com.example.pos.inventory.stockmovements.model.StockMovements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;

public interface StockMovementsRepository extends JpaRepository<StockMovements, UUID> {

    List<StockMovements> findByMedicineBatchesId(UUID batchId);

    @EntityGraph(attributePaths = {"medicineBatches", "medicineBatches.medicine", "user", "branch"})
    Page<StockMovements> findByMedicineBatchesIdAndBranchId(
            UUID batchId, UUID branchId, Pageable pageable);

    List<StockMovements> findByBranchId(UUID branchId);

    @EntityGraph(attributePaths = {"medicineBatches", "medicineBatches.medicine", "user", "branch"})
    Page<StockMovements> findByBranchId(UUID branchId, Pageable pageable);

    List<StockMovements> findByBranchIdAndMovementDateBetween(
            UUID branchId, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"medicineBatches", "medicineBatches.medicine", "user", "branch"})
    Page<StockMovements> findByBranchIdAndMovementDateBetween(
            UUID branchId, LocalDate start, LocalDate end, Pageable pageable);

    @EntityGraph(attributePaths = {"medicineBatches", "medicineBatches.medicine", "user", "branch"})
    java.util.Optional<StockMovements> findByIdAndBranchId(UUID id, UUID branchId);

    List<StockMovements> findByMovementType(StockMovements.MovementType movementType);

    List<StockMovements> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
