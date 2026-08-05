package com.example.pos.compliance.invoice.repository;

import java.util.UUID;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, UUID> {

    Optional<TaxInvoice> findByInvoiceNumber(String invoiceNumber);

    Optional<TaxInvoice> findBySaleId(UUID saleId);

    List<TaxInvoice> findByBranchIdAndInvoiceStatusIn(UUID branchId, List<InvoiceStatus> statuses);

    List<TaxInvoice> findByBranchIdAndCreatedAtBetween(UUID branchId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
