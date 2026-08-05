package com.example.pos.procurement.purchaseorders.repository;

import java.util.UUID;

import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, UUID> {

    List<PurchaseOrders> findBySupplierId(UUID supplierId);
    List<PurchaseOrders> findByBranchId(UUID branchId);
}
