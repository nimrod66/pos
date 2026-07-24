package com.example.pos.inventory.stockmovements.repository;

import com.example.pos.inventory.stockmovements.model.StockMovements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockMovementsRepository extends JpaRepository<StockMovements, Long> {

    List<StockMovements> findByMedicineBatchesId(Long batchId);

    Page<StockMovements> findByMedicineBatchesId(Long batchId, Pageable pageable);

    List<StockMovements> findByBranchId(Long branchId);

    Page<StockMovements> findByBranchId(Long branchId, Pageable pageable);

    List<StockMovements> findByBranchIdAndMovementDateBetween(
            Long branchId, LocalDate start, LocalDate end);

    Page<StockMovements> findByBranchIdAndMovementDateBetween(
            Long branchId, LocalDate start, LocalDate end, Pageable pageable);

    List<StockMovements> findByMovementType(StockMovements.MovementType movementType);

    List<StockMovements> findByReferenceTypeAndReferenceId(String referenceType, Integer referenceId);
}
