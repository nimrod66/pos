package com.example.pos.compliance.invoice.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.model.*;
import com.example.pos.compliance.invoice.repository.CreditNoteRepository;
import com.example.pos.compliance.invoice.repository.TaxInvoiceRepository;
import com.example.pos.compliance.numbering.service.DocumentNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepo;
    private final TaxInvoiceRepository invoiceRepo;
    private final DocumentNumberGenerator numberGenerator;

    public CreditNoteService(CreditNoteRepository creditNoteRepo,
                             TaxInvoiceRepository invoiceRepo,
                             DocumentNumberGenerator numberGenerator) {
        this.creditNoteRepo = creditNoteRepo;
        this.invoiceRepo = invoiceRepo;
        this.numberGenerator = numberGenerator;
    }

    public CreditNote create(UUID originalInvoiceId, BigDecimal amount, BigDecimal taxAmount,
                              String reason, UUID createdBy) {
        TaxInvoice original = invoiceRepo.findById(originalInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxInvoice", originalInvoiceId));

        if (original.getInvoiceStatus() != InvoiceStatus.ISSUED) {
            throw new BadRequestException("Credit notes can only be issued against ISSUED invoices");
        }

        List<CreditNote> existing = creditNoteRepo.findByOriginalInvoiceId(originalInvoiceId);
        BigDecimal totalCredited = existing.stream()
                .filter(cn -> cn.getStatus() != CreditNoteStatus.CANCELLED)
                .map(CreditNote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCredited.add(amount).compareTo(original.getGrandTotal()) > 0) {
            throw new BadRequestException("Total credit notes cannot exceed original invoice total of "
                    + original.getGrandTotal());
        }

        String branchCode = numberGenerator.generate("CN",
                "BR" + String.format("%03d", original.getBranchId() != null ? original.getBranchId() : 1));

        CreditNote cn = CreditNote.builder()
                .originalInvoiceId(originalInvoiceId)
                .creditNoteNumber(branchCode)
                .amount(amount)
                .taxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .reason(reason)
                .createdBy(createdBy)
                .build();

        return creditNoteRepo.save(cn);
    }

    public CreditNote issue(UUID id) {
        CreditNote cn = creditNoteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditNote", id));
        if (cn.getStatus() != CreditNoteStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT credit notes can be issued");
        }
        cn.setStatus(CreditNoteStatus.ISSUED);
        cn.setIssueDate(LocalDateTime.now());
        return creditNoteRepo.save(cn);
    }

    public CreditNote cancel(UUID id) {
        CreditNote cn = creditNoteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditNote", id));
        if (cn.getStatus() == CreditNoteStatus.CANCELLED) {
            throw new BadRequestException("Credit note is already cancelled");
        }
        cn.setStatus(CreditNoteStatus.CANCELLED);
        return creditNoteRepo.save(cn);
    }

    @Transactional(readOnly = true)
    public CreditNote getById(UUID id) {
        return creditNoteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreditNote", id));
    }

    @Transactional(readOnly = true)
    public List<CreditNote> getByInvoiceId(UUID invoiceId) {
        return creditNoteRepo.findByOriginalInvoiceId(invoiceId);
    }
}
