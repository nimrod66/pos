package com.example.pos.procurement.supplierinvoices.repository;

import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierInvoicesRepository extends JpaRepository<SupplierInvoices, Long> {

    List<SupplierInvoices> findBySuppliers_Id(Long supplierId);
}
