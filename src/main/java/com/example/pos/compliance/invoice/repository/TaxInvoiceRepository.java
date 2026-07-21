package com.example.pos.compliance.invoice.repository;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {

    Optional<TaxInvoice> findByInvoiceNumber(String invoiceNumber);

    Optional<TaxInvoice> findBySaleId(Long saleId);

    List<TaxInvoice> findByBranchIdAndInvoiceStatusIn(Long branchId, List<InvoiceStatus> statuses);

    List<TaxInvoice> findByBranchIdAndCreatedAtBetween(Long branchId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
