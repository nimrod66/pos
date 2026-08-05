package com.example.pos.sale.sales.repository;

import java.util.UUID;

import com.example.pos.sale.sales.model.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesRepository extends JpaRepository<Sales, UUID> {

    Optional<Sales> findByInvoiceNumber(String invoiceNumber);

    List<Sales> findByBranchId(UUID branchId);

    Page<Sales> findByBranchId(UUID branchId, Pageable pageable);

    List<Sales> findByUserId(UUID userId);

    List<Sales> findByBranchIdAndCreatedAtBetween(UUID branchId, LocalDateTime start, LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Optional<Sales> findTop1ByUserIdAndBranchIdOrderByCreatedAtDesc(UUID userId, UUID branchId);

    List<Sales> findByBranchIdAndSaleStatus(UUID branchId, Sales.SaleStatus saleStatus);

    List<Sales> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);
}
