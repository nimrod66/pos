package com.example.pos.procurement.purchaseorders.repository;

import java.util.UUID;

import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct po from PurchaseOrders po left join fetch po.purchaseOrderItems left join fetch po.supplier left join fetch po.branch where po.id = :id")
    java.util.Optional<PurchaseOrders> findForUpdateById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"supplier", "branch", "orderedBy", "approvedBy",
            "purchaseOrderItems", "purchaseOrderItems.medicine"})
    java.util.Optional<PurchaseOrders> findDetailedByIdAndBranchId(UUID id, UUID branchId);

    @Query("SELECT DISTINCT po FROM PurchaseOrders po LEFT JOIN FETCH po.purchaseOrderItems i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH po.supplier LEFT JOIN FETCH po.branch LEFT JOIN FETCH po.orderedBy LEFT JOIN FETCH po.approvedBy "
            + "WHERE po.branch.id = :branchId")
    Page<PurchaseOrders> findByBranchIdWithItems(@Param("branchId") UUID branchId, Pageable pageable);

    @Query("SELECT DISTINCT po FROM PurchaseOrders po LEFT JOIN FETCH po.purchaseOrderItems i LEFT JOIN FETCH i.medicine LEFT JOIN FETCH po.supplier LEFT JOIN FETCH po.branch LEFT JOIN FETCH po.orderedBy LEFT JOIN FETCH po.approvedBy "
            + "WHERE po.supplier.id = :supplierId AND po.branch.id = :branchId")
    Page<PurchaseOrders> findBySupplierIdAndBranchIdWithItems(@Param("supplierId") UUID supplierId,
                                                              @Param("branchId") UUID branchId,
                                                              Pageable pageable);

    Page<PurchaseOrders> findByBranchId(UUID branchId, Pageable pageable);

    Page<PurchaseOrders> findBySupplierIdAndBranchId(UUID supplierId, UUID branchId, Pageable pageable);

    List<PurchaseOrders> findBySupplierId(UUID supplierId);
    List<PurchaseOrders> findByBranchId(UUID branchId);
}
