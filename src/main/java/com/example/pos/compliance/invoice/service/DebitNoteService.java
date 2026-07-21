package com.example.pos.compliance.invoice.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.model.*;
import com.example.pos.compliance.invoice.repository.DebitNoteRepository;
import com.example.pos.compliance.invoice.repository.TaxInvoiceRepository;
import com.example.pos.compliance.numbering.service.DocumentNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DebitNoteService {

    private final DebitNoteRepository debitNoteRepo;
    private final TaxInvoiceRepository invoiceRepo;
    private final DocumentNumberGenerator numberGenerator;

    public DebitNoteService(DebitNoteRepository debitNoteRepo,
                            TaxInvoiceRepository invoiceRepo,
                            DocumentNumberGenerator numberGenerator) {
        this.debitNoteRepo = debitNoteRepo;
        this.invoiceRepo = invoiceRepo;
        this.numberGenerator = numberGenerator;
    }

    public DebitNote create(Long originalInvoiceId, BigDecimal amount, BigDecimal taxAmount,
                             String reason, Long createdBy) {
        TaxInvoice original = invoiceRepo.findById(originalInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxInvoice", originalInvoiceId));

        if (original.getInvoiceStatus() != InvoiceStatus.ISSUED) {
            throw new BadRequestException("Debit notes can only be issued against ISSUED invoices");
        }

        DebitNote dn = DebitNote.builder()
                .originalInvoiceId(originalInvoiceId)
                .debitNoteNumber(numberGenerator.generate("DN",
                        "BR" + String.format("%03d", original.getBranchId() != null ? original.getBranchId() : 1)))
                .amount(amount)
                .taxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .reason(reason)
                .createdBy(createdBy)
                .build();

        return debitNoteRepo.save(dn);
    }

    public DebitNote issue(Long id) {
        DebitNote dn = debitNoteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", id));
        if (dn.getStatus() != CreditNoteStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT debit notes can be issued");
        }
        dn.setStatus(CreditNoteStatus.ISSUED);
        dn.setIssueDate(LocalDateTime.now());
        return debitNoteRepo.save(dn);
    }

    public DebitNote cancel(Long id) {
        DebitNote dn = debitNoteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", id));
        if (dn.getStatus() == CreditNoteStatus.CANCELLED) {
            throw new BadRequestException("Debit note is already cancelled");
        }
        dn.setStatus(CreditNoteStatus.CANCELLED);
        return debitNoteRepo.save(dn);
    }

    @Transactional(readOnly = true)
    public DebitNote getById(Long id) {
        return debitNoteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DebitNote", id));
    }

    @Transactional(readOnly = true)
    public List<DebitNote> getByInvoiceId(Long invoiceId) {
        return debitNoteRepo.findByOriginalInvoiceId(invoiceId);
    }
}
