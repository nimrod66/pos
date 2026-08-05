package com.example.pos.procurement.supplierinvoices.repository;

import java.util.UUID;

import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierInvoicesRepository extends JpaRepository<SupplierInvoices, UUID> {

    List<SupplierInvoices> findBySuppliers_Id(UUID supplierId);
}
