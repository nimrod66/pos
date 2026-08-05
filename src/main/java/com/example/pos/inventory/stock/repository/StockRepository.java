package com.example.pos.inventory.stock.repository;

import java.util.UUID;

import com.example.pos.inventory.stock.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    List<Stock> findByBranchId(UUID branchId);

    Page<Stock> findByBranchId(UUID branchId, Pageable pageable);

    Optional<Stock> findByMedicineBatchesId(UUID batchId);

    Optional<Stock> findByBranchIdAndMedicineBatchesId(UUID branchId, UUID batchId);

    List<Stock> findByBranchIdAndQuantityAvailableLessThanEqual(UUID branchId, Integer threshold);

    List<Stock> findByBranchIdAndMedicineBatches_Medicine_Id(UUID branchId, UUID medicineId);

    List<Stock> findByBranchIdAndQuantityAvailableGreaterThan(UUID branchId, int minQuantity);
}
