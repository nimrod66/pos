package com.example.pos.inventory.stockmovements.repository;

import java.util.UUID;

import com.example.pos.inventory.stockmovements.model.StockMovements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockMovementsRepository extends JpaRepository<StockMovements, UUID> {

    List<StockMovements> findByMedicineBatchesId(UUID batchId);

    Page<StockMovements> findByMedicineBatchesId(UUID batchId, Pageable pageable);

    List<StockMovements> findByBranchId(UUID branchId);

    Page<StockMovements> findByBranchId(UUID branchId, Pageable pageable);

    List<StockMovements> findByBranchIdAndMovementDateBetween(
            UUID branchId, LocalDate start, LocalDate end);

    Page<StockMovements> findByBranchIdAndMovementDateBetween(
            UUID branchId, LocalDate start, LocalDate end, Pageable pageable);

    List<StockMovements> findByMovementType(StockMovements.MovementType movementType);

    List<StockMovements> findByReferenceTypeAndReferenceId(String referenceType, Integer referenceId);
}
