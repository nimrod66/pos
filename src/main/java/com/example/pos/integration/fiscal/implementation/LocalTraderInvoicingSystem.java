package com.example.pos.integration.fiscal.implementation;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.tis.TisFacade;
import com.example.pos.compliance.tis.TraderInvoicingSystem;

import java.math.BigDecimal;
import java.util.UUID;

public class LocalTraderInvoicingSystem implements TraderInvoicingSystem {

    private final TisFacade tisFacade;

    public LocalTraderInvoicingSystem(TisFacade tisFacade) {
        this.tisFacade = tisFacade;
    }

    @Override
    public TaxInvoice generateInvoice(UUID saleId, String customerPin) {
        return tisFacade.generateInvoice(saleId, customerPin);
    }

    @Override
    public String generateReceipt(UUID saleId, UUID invoiceId) {
        return tisFacade.generateReceipt(saleId, invoiceId);
    }

    @Override
    public void validateInvoice(TaxInvoice invoice) {
        tisFacade.validateInvoice(invoice);
    }

    @Override
    public void submitToKra(TaxInvoice invoice) {
        tisFacade.submitToKra(invoice);
    }

    @Override
    public void cancelInvoice(UUID invoiceId, String reason) {
        tisFacade.cancelInvoice(invoiceId, reason);
    }

    @Override
    public void issueCreditNote(UUID invoiceId, BigDecimal amount, String reason) {
        tisFacade.issueCreditNote(invoiceId, amount, reason);
    }

    @Override
    public void issueDebitNote(UUID invoiceId, BigDecimal amount, String reason) {
        tisFacade.issueDebitNote(invoiceId, amount, reason);
    }

    @Override
    public void reconciliationCheck() {
        tisFacade.reconciliationCheck();
    }
}
