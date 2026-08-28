package com.example.pos.sale.sales.repository;

import java.util.UUID;

import com.example.pos.customer.model.Customer;
import com.example.pos.sale.sales.model.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesRepository extends JpaRepository<Sales, UUID> {

    Optional<Sales> findByInvoiceNumber(String invoiceNumber);

    List<Sales> findByBranchId(UUID branchId);

    Page<Sales> findByBranchId(UUID branchId, Pageable pageable);

    List<Sales> findByUserId(UUID userId);

    List<Sales> findByBranchIdAndCreatedAtBetween(UUID branchId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"payment", "saleItems", "saleItems.medicineBatches",
            "saleItems.medicineBatches.medicine"})
    List<Sales> findByBranchIdInAndSaleStatusInAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            List<UUID> branchIds, List<Sales.SaleStatus> statuses,
            LocalDateTime start, LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByCustomerId(UUID customerId);

    Optional<Sales> findTop1ByUserIdAndBranchIdOrderByCreatedAtDesc(UUID userId, UUID branchId);

    @EntityGraph(attributePaths = {"branch", "user", "customer", "prescription", "shift", "saleItems",
            "saleItems.medicineBatches", "saleItems.medicineBatches.medicine", "saleItems.saleReturnItems",
            "saleItems.saleReturnItems.saleReturns", "payment", "receipts"})
    Optional<Sales> findDetailedByIdAndBranchId(UUID id, UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sale from Sales sale where sale.id = :id and sale.branch.id = :branchId")
    Optional<Sales> findForUpdateByIdAndBranchId(
            @Param("id") UUID id, @Param("branchId") UUID branchId);

    List<Sales> findByBranchIdAndSaleStatus(UUID branchId, Sales.SaleStatus saleStatus);

    List<Sales> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"saleItems", "saleItems.medicineBatches", "saleItems.medicineBatches.medicine", "payment"})
    List<Sales> findByCustomerIdAndBranchIdOrderByCompletedAtDesc(UUID customerId, UUID branchId);

    List<Sales> findByCustomerAndAmountOwedGreaterThan(Customer customer, BigDecimal amountOwed);
}
