package com.example.pos.compliance.invoice.repository;

import com.example.pos.compliance.invoice.model.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {

    Optional<DebitNote> findByDebitNoteNumber(String debitNoteNumber);

    List<DebitNote> findByOriginalInvoiceId(Long originalInvoiceId);
}
