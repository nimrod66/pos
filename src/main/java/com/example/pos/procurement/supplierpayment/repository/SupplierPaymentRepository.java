package com.example.pos.procurement.supplierpayment.repository;

import java.util.UUID;

import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, UUID> {

    List<SupplierPayment> findBySupplierInvoicesId(UUID invoiceId);
}
