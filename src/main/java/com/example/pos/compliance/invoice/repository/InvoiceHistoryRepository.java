package com.example.pos.compliance.invoice.repository;

import com.example.pos.compliance.invoice.model.InvoiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceHistoryRepository extends JpaRepository<InvoiceHistory, Long> {

    List<InvoiceHistory> findByInvoiceIdOrderByCreatedAtAsc(Long invoiceId);
}
