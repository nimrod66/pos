package com.example.pos.compliance.tis;

import com.example.pos.compliance.config.ComplianceConfiguration;
import com.example.pos.compliance.gateway.ComplianceGateway;
import com.example.pos.compliance.gateway.ComplianceGatewayFactory;
import com.example.pos.compliance.health.ComplianceHealthService;
import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.service.InvoiceService;
import com.example.pos.compliance.numbering.service.DocumentNumberGenerator;
import com.example.pos.compliance.reconciliation.service.ReconciliationService;
import com.example.pos.compliance.receipt.service.ComplianceReceiptService;
import com.example.pos.compliance.transmission.service.TransmissionService;
import com.example.pos.compliance.validation.ComplianceValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class TisFacade implements TraderInvoicingSystem {

    private final InvoiceService invoiceService;
    private final ComplianceReceiptService receiptService;
    private final ComplianceValidationService validationService;
    private final TransmissionService transmissionService;
    private final ComplianceGatewayFactory gatewayFactory;
    private final DocumentNumberGenerator numberGenerator;
    private final ComplianceConfiguration config;
    private final ComplianceHealthService healthService;
    private final ReconciliationService reconciliationService;

    public TisFacade(InvoiceService invoiceService,
                     ComplianceReceiptService receiptService,
                     ComplianceValidationService validationService,
                     TransmissionService transmissionService,
                     ComplianceGatewayFactory gatewayFactory,
                     DocumentNumberGenerator numberGenerator,
                     ComplianceConfiguration config,
                     ComplianceHealthService healthService,
                     ReconciliationService reconciliationService) {
        this.invoiceService = invoiceService;
        this.receiptService = receiptService;
        this.validationService = validationService;
        this.transmissionService = transmissionService;
        this.gatewayFactory = gatewayFactory;
        this.numberGenerator = numberGenerator;
        this.config = config;
        this.healthService = healthService;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public TaxInvoice generateInvoice(Long saleId, String customerPin) {
        return invoiceService.issueFromSale(saleId, customerPin, "KES", null, "TIS");
    }

    @Override
    public String generateReceipt(Long saleId, Long invoiceId) {
        var receipt = receiptService.create(saleId, invoiceId, "{}", "Pharmacy", config.getKraPin(),
                "BR001", null, null);
        return receipt.getReceiptNumber();
    }

    @Override
    public void validateInvoice(TaxInvoice invoice) {
        var report = validationService.validate(invoice);
        if (!report.isValid()) {
            throw new IllegalStateException("Invoice validation failed: " + report.getErrors());
        }
    }

    @Override
    public void submitToKra(TaxInvoice invoice) {
        ComplianceGateway gateway = gatewayFactory.getForInvoice(invoice);
        queueTransmission(invoice, gateway);
    }

    @Override
    public void cancelInvoice(Long invoiceId, String reason) {
        invoiceService.cancel(invoiceId, reason, null, "TIS");
    }

    @Override
    public void issueCreditNote(Long invoiceId, BigDecimal amount, String reason) {
        throw new UnsupportedOperationException("Use CreditNoteService directly");
    }

    @Override
    public void issueDebitNote(Long invoiceId, BigDecimal amount, String reason) {
        throw new UnsupportedOperationException("Use DebitNoteService directly");
    }

    @Override
    public void reconciliationCheck() {
        reconciliationService.runReconciliation();
    }

    private void queueTransmission(TaxInvoice invoice, ComplianceGateway gateway) {
        transmissionService.createAndQueue(invoice.getId(), "TAX_INVOICE", null);
    }
}
