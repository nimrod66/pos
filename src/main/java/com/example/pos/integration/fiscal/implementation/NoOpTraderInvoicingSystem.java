package com.example.pos.integration.fiscal.implementation;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.tis.TraderInvoicingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class NoOpTraderInvoicingSystem implements TraderInvoicingSystem {

    private static final Logger log = LoggerFactory.getLogger(NoOpTraderInvoicingSystem.class);

    @Override
    public TaxInvoice generateInvoice(Long saleId, String customerPin) {
        log.info("Fiscal integration disabled — skipping invoice for sale {}", saleId);
        return null;
    }

    @Override
    public String generateReceipt(Long saleId, Long invoiceId) {
        log.info("Fiscal integration disabled — skipping receipt for sale {}", saleId);
        return "DISABLED";
    }

    @Override
    public void validateInvoice(TaxInvoice invoice) {
    }

    @Override
    public void submitToKra(TaxInvoice invoice) {
        log.info("Fiscal integration disabled — skipping KRA submission");
    }

    @Override
    public void cancelInvoice(Long invoiceId, String reason) {
        log.info("Fiscal integration disabled — skipping cancellation of invoice {}", invoiceId);
    }

    @Override
    public void issueCreditNote(Long invoiceId, BigDecimal amount, String reason) {
        log.info("Fiscal integration disabled — skipping credit note for invoice {}", invoiceId);
    }

    @Override
    public void issueDebitNote(Long invoiceId, BigDecimal amount, String reason) {
        log.info("Fiscal integration disabled — skipping debit note for invoice {}", invoiceId);
    }

    @Override
    public void reconciliationCheck() {
        log.debug("Fiscal integration disabled — skipping reconciliation");
    }
}
