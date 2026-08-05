package com.example.pos.compliance.invoice.repository;

import java.util.UUID;

import com.example.pos.compliance.invoice.model.TaxInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxInvoiceItemRepository extends JpaRepository<TaxInvoiceItem, UUID> {

    List<TaxInvoiceItem> findByTaxInvoiceId(UUID taxInvoiceId);
}
