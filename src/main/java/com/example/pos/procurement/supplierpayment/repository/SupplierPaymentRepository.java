package com.example.pos.procurement.supplierpayment.repository;

import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findBySupplierInvoicesId(Long invoiceId);
}
