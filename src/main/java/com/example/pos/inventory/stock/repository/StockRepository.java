package com.example.pos.inventory.stock.repository;

import com.example.pos.inventory.stock.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findByBranchId(Long branchId);

    List<Stock> findByMedicineBatchesId(Long batchId);

    Optional<Stock> findByBranchIdAndMedicineBatchesId(Long branchId, Long batchId);

    List<Stock> findByBranchIdAndQuantityAvailableLessThanEqual(Long branchId, Integer threshold);

    List<Stock> findByBranchIdAndMedicineBatches_Medicine_Id(Long branchId, Long medicineId);

    List<Stock> findByBranchIdAndQuantityAvailableGreaterThan(Long branchId, int minQuantity);
}
