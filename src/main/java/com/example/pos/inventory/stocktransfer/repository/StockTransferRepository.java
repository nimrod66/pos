package com.example.pos.inventory.stocktransfer.repository;

import com.example.pos.inventory.stocktransfer.model.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {

    Page<StockTransfer> findBySourceBranchIdOrderByCreatedAtDesc(UUID branchId, Pageable pageable);

    Page<StockTransfer> findByDestBranchIdOrderByCreatedAtDesc(UUID branchId, Pageable pageable);

    Page<StockTransfer> findBySourceBranchIdOrDestBranchIdOrderByCreatedAtDesc(UUID branchId1, UUID branchId2, Pageable pageable);

    List<StockTransfer> findBySourceBranchIdOrDestBranchIdAndStatus(UUID sourceBranchId, UUID destBranchId, String status);
}
