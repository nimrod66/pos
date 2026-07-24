package com.example.pos.sale.sales.repository;

import com.example.pos.sale.sales.model.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesRepository extends JpaRepository<Sales, Long> {

    Optional<Sales> findByInvoiceNumber(String invoiceNumber);

    List<Sales> findByBranchId(Long branchId);

    Page<Sales> findByBranchId(Long branchId, Pageable pageable);

    List<Sales> findByUserId(Long userId);

    List<Sales> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime start, LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);

    Optional<Sales> findTop1ByUserIdAndBranchIdOrderByCreatedAtDesc(Long userId, Long branchId);

    List<Sales> findByBranchIdAndSaleStatus(Long branchId, Sales.SaleStatus saleStatus);

    List<Sales> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
