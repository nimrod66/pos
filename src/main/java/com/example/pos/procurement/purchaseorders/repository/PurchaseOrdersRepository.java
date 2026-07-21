package com.example.pos.procurement.purchaseorders.repository;

import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrdersRepository extends JpaRepository<PurchaseOrders, Long> {

    List<PurchaseOrders> findBySupplierId(Long supplierId);
    List<PurchaseOrders> findByBranchId(Long branchId);
}
