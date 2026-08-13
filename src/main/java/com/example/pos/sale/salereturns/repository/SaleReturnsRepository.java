package com.example.pos.sale.salereturns.repository;

import java.util.UUID;
import java.time.LocalDateTime;

import com.example.pos.sale.salereturns.model.SaleReturns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface SaleReturnsRepository extends JpaRepository<SaleReturns, UUID> {

    List<SaleReturns> findBySalesId(UUID saleId);

    @EntityGraph(attributePaths = {"sales", "user", "branch", "staffShift"})
    Page<SaleReturns> findBySalesIdAndBranchId(UUID saleId, UUID branchId, Pageable pageable);

    @EntityGraph(attributePaths = {"sales", "user", "branch", "staffShift", "saleReturnItems",
            "saleReturnItems.saleItems", "saleReturnItems.medicineBatches",
            "saleReturnItems.medicineBatches.medicine"})
    Optional<SaleReturns> findDetailedByIdAndBranchId(UUID id, UUID branchId);

    @EntityGraph(attributePaths = {"sales", "user", "branch", "staffShift", "saleReturnItems",
            "saleReturnItems.saleItems", "saleReturnItems.medicineBatches",
            "saleReturnItems.medicineBatches.medicine"})
    Optional<SaleReturns> findDetailedByClientReturnIdAndBranchId(UUID clientReturnId, UUID branchId);

    @EntityGraph(attributePaths = {"saleReturnItems", "saleReturnItems.medicineBatches",
            "saleReturnItems.medicineBatches.medicine"})
    List<SaleReturns> findByBranchIdInAndStatusIgnoreCaseAndReturnDateGreaterThanEqualAndReturnDateLessThan(
            List<UUID> branchIds, String status, LocalDateTime start, LocalDateTime end);

    boolean existsByRefundMethodAndRefundReferenceIgnoreCase(
            String refundMethod, String refundReference);
}
