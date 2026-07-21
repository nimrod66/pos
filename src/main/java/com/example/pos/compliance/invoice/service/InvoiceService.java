package com.example.pos.compliance.invoice.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.compliance.invoice.event.InvoiceIssuedEvent;
import com.example.pos.compliance.invoice.model.*;
import com.example.pos.compliance.invoice.repository.*;
import com.example.pos.compliance.numbering.service.DocumentNumberGenerator;
import com.example.pos.compliance.tax.service.TaxEngine;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.sale.saleitems.model.SaleItems;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InvoiceService {

    private final TaxInvoiceRepository invoiceRepo;
    private final TaxInvoiceItemRepository itemRepo;
    private final InvoiceHistoryRepository historyRepo;
    private final SalesRepository salesRepo;
    private final DocumentNumberGenerator numberGenerator;
    private final TaxEngine taxEngine;
    private final ApplicationEventPublisher eventPublisher;

    public InvoiceService(TaxInvoiceRepository invoiceRepo,
                          TaxInvoiceItemRepository itemRepo,
                          InvoiceHistoryRepository historyRepo,
                          SalesRepository salesRepo,
                          DocumentNumberGenerator numberGenerator,
                          TaxEngine taxEngine,
                          ApplicationEventPublisher eventPublisher) {
        this.invoiceRepo = invoiceRepo;
        this.itemRepo = itemRepo;
        this.historyRepo = historyRepo;
        this.salesRepo = salesRepo;
        this.numberGenerator = numberGenerator;
        this.taxEngine = taxEngine;
        this.eventPublisher = eventPublisher;
    }

    public TaxInvoice issueFromSale(Long saleId, String customerPin, String currency, Long actorId, String actorName) {
        Sales sale = salesRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));

        if (invoiceRepo.findBySaleId(sale.getId()).isPresent()) {
            throw new BadRequestException("An invoice already exists for sale " + sale.getId());
        }

        if (sale.getSaleStatus() == Sales.SaleStatus.CANCELLED) {
            throw new BadRequestException("Cannot issue invoice for a cancelled sale");
        }

        if (sale.getBranch() == null) {
            throw new BadRequestException("Sale has no associated branch");
        }

        String branchCode = sale.getBranch().getBranchCode() != null
                ? sale.getBranch().getBranchCode()
                : "BR" + String.format("%03d", sale.getBranch().getId());

        TaxInvoice invoice = new TaxInvoice();
        invoice.setSale(sale);
        invoice.setInvoiceNumber(numberGenerator.generate("INV", branchCode));
        invoice.setInvoiceStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setCurrency(currency != null ? currency : "KES");
        invoice.setBranchId(sale.getBranch().getId());
        invoice.setSubtotal(sale.getSubtotal());

        if (sale.getCustomer() != null) {
            invoice.setCustomerId(sale.getCustomer().getId());
            invoice.setCustomerName(sale.getCustomer().getFirstName()
                    + (sale.getCustomer().getLastName() != null ? " " + sale.getCustomer().getLastName() : ""));
        }
        invoice.setCustomerPin(customerPin);

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<TaxInvoiceItem> items = new ArrayList<>();

        for (SaleItems si : sale.getSaleItems()) {
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

        recordHistory(invoice, InvoiceHistoryType.ISSUED, "Invoice issued from sale " + saleId, actorId, actorName);

        eventPublisher.publishEvent(new InvoiceIssuedEvent(this, invoice));

        return invoice;
    }

    @Transactional(readOnly = true)
    public TaxInvoice getById(Long id) {
        TaxInvoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxInvoice", id));
        invoice.setItems(itemRepo.findByTaxInvoiceId(id));
        return invoice;
    }

    @Transactional(readOnly = true)
    public TaxInvoice getBySaleId(Long saleId) {
        TaxInvoice invoice = invoiceRepo.findBySaleId(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxInvoice for sale", saleId));
        invoice.setItems(itemRepo.findByTaxInvoiceId(invoice.getId()));
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<TaxInvoice> getByBranch(Long branchId, List<InvoiceStatus> statuses) {
        List<TaxInvoice> invoices;
        if (statuses != null && !statuses.isEmpty()) {
            invoices = invoiceRepo.findByBranchIdAndInvoiceStatusIn(branchId, statuses);
        } else {
            invoices = invoiceRepo.findAll();
        }
        invoices.forEach(inv -> inv.setItems(itemRepo.findByTaxInvoiceId(inv.getId())));
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<TaxInvoice> getByBranchAndDate(Long branchId, LocalDateTime start, LocalDateTime end) {
        List<TaxInvoice> invoices = invoiceRepo.findByBranchIdAndCreatedAtBetween(branchId, start, end);
        invoices.forEach(inv -> inv.setItems(itemRepo.findByTaxInvoiceId(inv.getId())));
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<InvoiceHistory> getHistory(Long invoiceId) {
        return historyRepo.findByInvoiceIdOrderByCreatedAtAsc(invoiceId);
    }

    public TaxInvoice cancel(Long id, String reason, Long actorId, String actorName) {
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

    public void recordTransmissionSent(Long invoiceId, String description, Long actorId, String actorName) {
        TaxInvoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        if (invoice != null) {
            recordHistory(invoice, InvoiceHistoryType.SENT_TO_KRA, description, actorId, actorName);
        }
    }

    public void recordTransmissionAcknowledged(Long invoiceId, String kraReceiptNumber, Long actorId, String actorName) {
        TaxInvoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        if (invoice != null) {
            recordHistory(invoice, InvoiceHistoryType.ACKNOWLEDGED,
                    "KRA receipt: " + kraReceiptNumber, actorId, actorName);
        }
    }

    public void recordTransmissionFailed(Long invoiceId, String failureReason, Long actorId, String actorName) {
        TaxInvoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        if (invoice != null) {
            recordHistory(invoice, InvoiceHistoryType.TRANSMISSION_FAILED,
                    failureReason, actorId, actorName);
        }
    }

    private void recordHistory(TaxInvoice invoice, InvoiceHistoryType type, String description,
                                Long actorId, String actorName) {
        InvoiceHistory history = InvoiceHistory.builder()
                .invoice(invoice)
                .historyType(type)
                .description(description)
                .actorId(actorId)
                .actorName(actorName)
                .build();
        historyRepo.save(history);
    }

    private TaxInvoiceItem buildInvoiceItem(TaxInvoice invoice, SaleItems si) {
        Medicine medicine = si.getMedicineBatches() != null ? si.getMedicineBatches().getMedicine() : null;
        Tax taxCategory = medicine != null ? medicine.getTax() : null;

        BigDecimal lineSubtotal = si.getPrice().multiply(BigDecimal.valueOf(si.getQuantity()));
        BigDecimal lineDiscount = si.getDiscount() != null ? si.getDiscount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = taxEngine.calculateTaxableAmount(si.getPrice(), si.getQuantity(), lineDiscount);
        BigDecimal taxRate = taxCategory != null ? taxCategory.getTaxRate() : BigDecimal.ZERO;
        BigDecimal lineTax = si.getTax() != null ? si.getTax() : BigDecimal.ZERO;
        BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount).add(lineTax);

        return TaxInvoiceItem.builder()
                .taxInvoice(invoice)
                .medicineId(medicine != null ? medicine.getId() : null)
                .medicineName(medicine != null ? medicine.getBrandName() : null)
                .barcode(medicine != null ? medicine.getBarcode() : null)
                .quantity(si.getQuantity())
                .unitPrice(si.getPrice())
                .taxableAmount(taxableAmount)
                .taxRate(taxRate)
                .taxType(taxCategory != null ? taxCategory.getCode() : null)
                .taxAmount(lineTax)
                .discount(lineDiscount)
                .subtotal(lineSubtotal)
                .total(lineTotal)
                .build();
    }
}
