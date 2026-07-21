package com.example.pos.compliance.invoice.repository;

import com.example.pos.compliance.invoice.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);

    List<CreditNote> findByOriginalInvoiceId(Long originalInvoiceId);
}
