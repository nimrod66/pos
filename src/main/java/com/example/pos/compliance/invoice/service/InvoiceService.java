package com.example.pos.compliance.invoice.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.dto.SaleFiscalData;
import com.example.pos.compliance.invoice.dto.SaleFiscalItemData;
import com.example.pos.compliance.invoice.event.InvoiceIssuedEvent;
import com.example.pos.compliance.invoice.model.*;
import com.example.pos.compliance.invoice.repository.*;
import com.example.pos.compliance.numbering.service.DocumentNumberGenerator;
import com.example.pos.compliance.tax.dto.TaxSnapshot;
import com.example.pos.compliance.tax.service.TaxEngine;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final TaxInvoiceRepository invoiceRepo;
    private final TaxInvoiceItemRepository itemRepo;
    private final InvoiceHistoryRepository historyRepo;
    private final DocumentNumberGenerator numberGenerator;
    private final TaxEngine taxEngine;
    private final ApplicationEventPublisher eventPublisher;

    public InvoiceService(TaxInvoiceRepository invoiceRepo,
                          TaxInvoiceItemRepository itemRepo,
                          InvoiceHistoryRepository historyRepo,
                          DocumentNumberGenerator numberGenerator,
                          TaxEngine taxEngine,
                          ApplicationEventPublisher eventPublisher) {
        this.invoiceRepo = invoiceRepo;
        this.itemRepo = itemRepo;
        this.historyRepo = historyRepo;
        this.numberGenerator = numberGenerator;
        this.taxEngine = taxEngine;
        this.eventPublisher = eventPublisher;
    }

    public TaxInvoice issueFromSale(SaleFiscalData saleData, UUID actorId, String actorName) {
        if (invoiceRepo.findBySaleId(saleData.saleId()).isPresent()) {
            throw new BadRequestException("An invoice already exists for sale " + saleData.saleId());
        }

        if (saleData.cancelled()) {
            throw new BadRequestException("Cannot issue invoice for a cancelled sale");
        }

        if (saleData.branchCode() == null) {
            throw new BadRequestException("Sale has no associated branch code");
        }

        String branchCode = saleData.branchCode();

        TaxInvoice invoice = new TaxInvoice();
        invoice.setSaleId(saleData.saleId());
        invoice.setBranchCode(branchCode);
        invoice.setInvoiceNumber(numberGenerator.generate("INV", branchCode));
        invoice.setInvoiceStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setCurrency(saleData.currency() != null ? saleData.currency() : "KES");
        invoice.setBranchId(saleData.branchId());
        invoice.setSubtotal(saleData.subtotal());

        if (saleData.customerId() != null) {
            invoice.setCustomerId(saleData.customerId());
            invoice.setCustomerName(saleData.customerName());
        }
        invoice.setCustomerPin(saleData.customerPin());

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<TaxInvoiceItem> items = new ArrayList<>();

        for (SaleFiscalItemData si : saleData.items()) {
            TaxInvoiceItem item = buildInvoiceItem(invoice, si);
            items.add(item);
            totalTax = totalTax.add(item.getTaxAmount());
            totalDiscount = totalDiscount.add(item.getDiscount());
        }

        invoice.setTaxAmount(totalTax);
        invoice.setDiscount(totalDiscount);
        invoice.setGrandTotal(invoice.getSubtotal()
                .subtract(totalDiscount)
                .add(totalTax)
                .setScale(2, RoundingMode.HALF_UP));

        invoice = invoiceRepo.save(invoice);

        for (TaxInvoiceItem item : items) {
            item.setTaxInvoice(invoice);
            itemRepo.save(item);
        }
        invoice.setItems(items);

        recordHistory(invoice, InvoiceHistoryType.ISSUED, "Invoice issued from sale " + saleData.saleId(), actorId, actorName);

        eventPublisher.publishEvent(new InvoiceIssuedEvent(this, invoice));

        return invoice;
    }

    @Transactional(readOnly = true)
    public TaxInvoice getById(UUID id) {
        TaxInvoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxInvoice", id));
        invoice.setItems(itemRepo.findByTaxInvoiceId(id));
        return invoice;
    }

    @Transactional(readOnly = true)
    public TaxInvoice getBySaleId(UUID saleId) {
        TaxInvoice invoice = invoiceRepo.findBySaleId(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxInvoice for sale", saleId));
        invoice.setItems(itemRepo.findByTaxInvoiceId(invoice.getId()));
        return invoice;
    }

    @Transactional(readOnly = true)
    public Page<TaxInvoice> getByBranch(UUID branchId, List<InvoiceStatus> statuses, Pageable pageable) {
        Page<TaxInvoice> page;
        if (statuses != null && !statuses.isEmpty()) {
            List<TaxInvoice> invoices = invoiceRepo.findByBranchIdAndInvoiceStatusIn(branchId, statuses);
            page = new PageImpl<>(invoices, pageable, invoices.size());
        } else {
            page = invoiceRepo.findAll(pageable);
        }
        page.getContent().forEach(inv -> inv.setItems(itemRepo.findByTaxInvoiceId(inv.getId())));
        return page;
    }

    @Transactional(readOnly = true)
    public Page<TaxInvoice> getByBranchAndDate(UUID branchId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        List<TaxInvoice> invoices = invoiceRepo.findByBranchIdAndCreatedAtBetween(branchId, start, end);
        Page<TaxInvoice> page = new PageImpl<>(invoices, pageable, invoices.size());
        page.getContent().forEach(inv -> inv.setItems(itemRepo.findByTaxInvoiceId(inv.getId())));
        return page;
    }

    @Transactional(readOnly = true)
    public List<InvoiceHistory> getHistory(UUID invoiceId) {
        return historyRepo.findByInvoiceIdOrderByCreatedAtAsc(invoiceId);
    }

    public TaxInvoice cancel(UUID id, String reason, UUID actorId, String actorName) {
        TaxInvoice invoice = getById(id);
        if (invoice.getInvoiceStatus() == InvoiceStatus.VOID) {
            throw new BadRequestException("Invoice is already voided");
        }
        invoice.setInvoiceStatus(InvoiceStatus.VOID);
        invoice = invoiceRepo.save(invoice);
        recordHistory(invoice, InvoiceHistoryType.VOID, reason, actorId, actorName);
        invoice.setItems(itemRepo.findByTaxInvoiceId(invoice.getId()));
        return invoice;
    }

    public void recordTransmissionSent(UUID invoiceId, String description, UUID actorId, String actorName) {
        TaxInvoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        if (invoice != null) {
            recordHistory(invoice, InvoiceHistoryType.SENT_TO_KRA, description, actorId, actorName);
        }
    }

    public void recordTransmissionAcknowledged(UUID invoiceId, String kraReceiptNumber, UUID actorId, String actorName) {
        TaxInvoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        if (invoice != null) {
            recordHistory(invoice, InvoiceHistoryType.ACKNOWLEDGED,
                    "KRA receipt: " + kraReceiptNumber, actorId, actorName);
        }
    }

    public void recordTransmissionFailed(UUID invoiceId, String failureReason, UUID actorId, String actorName) {
        TaxInvoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        if (invoice != null) {
            recordHistory(invoice, InvoiceHistoryType.TRANSMISSION_FAILED,
                    failureReason, actorId, actorName);
        }
    }

    private void recordHistory(TaxInvoice invoice, InvoiceHistoryType type, String description,
                                UUID actorId, String actorName) {
        InvoiceHistory history = InvoiceHistory.builder()
                .invoice(invoice)
                .historyType(type)
                .description(description)
                .actorId(actorId)
                .actorName(actorName)
                .build();
        historyRepo.save(history);
    }

    private TaxInvoiceItem buildInvoiceItem(TaxInvoice invoice, SaleFiscalItemData si) {
        BigDecimal lineSubtotal = si.unitPrice().multiply(BigDecimal.valueOf(si.quantity()));
        BigDecimal lineDiscount = si.discount() != null ? si.discount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = taxEngine.calculateTaxableAmount(si.unitPrice(), si.quantity(), lineDiscount);
        BigDecimal taxRate = si.taxRate() != null ? si.taxRate() : BigDecimal.ZERO;
        BigDecimal lineTax = si.taxAmount() != null ? si.taxAmount() : BigDecimal.ZERO;
        BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount).add(lineTax);

        return TaxInvoiceItem.builder()
                .taxInvoice(invoice)
                .medicineId(si.medicineId())
                .medicineName(si.medicineName())
                .barcode(si.barcode())
                .barcodeType(si.barcodeType())
                .etimsClassificationCode(si.etimsClassificationCode())
                .quantity(si.quantity())
                .unitPrice(si.unitPrice())
                .taxableAmount(taxableAmount)
                .taxRate(taxRate)
                .taxType(si.taxCode())
                .taxAmount(lineTax)
                .discount(lineDiscount)
                .subtotal(lineSubtotal)
                .total(lineTotal)
                .build();
    }
}