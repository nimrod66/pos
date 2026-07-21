package com.example.pos.inventory.stockmovements.repository;

import com.example.pos.inventory.stockmovements.model.StockMovements;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockMovementsRepository extends JpaRepository<StockMovements, Long> {

    List<StockMovements> findByMedicineBatchesId(Long batchId);

    List<StockMovements> findByBranchId(Long branchId);

    List<StockMovements> findByBranchIdAndMovementDateBetween(
            Long branchId, LocalDate start, LocalDate end);

    List<StockMovements> findByMovementType(StockMovements.MovementType movementType);

    List<StockMovements> findByReferenceTypeAndReferenceId(String referenceType, Integer referenceId);
}
