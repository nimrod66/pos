package com.example.pos.inventory.stockcount.repository;

import com.example.pos.inventory.stockcount.model.StockCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockCountRepository extends JpaRepository<StockCount, UUID> {

    Page<StockCount> findByBranchIdOrderByCountDateDesc(UUID branchId, Pageable pageable);

    Optional<StockCount> findByBranchIdAndCountDate(UUID branchId, LocalDate countDate);

    List<StockCount> findByBranchIdAndStatus(UUID branchId, String status);
}
