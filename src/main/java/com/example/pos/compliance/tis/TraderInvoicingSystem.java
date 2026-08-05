package com.example.pos.compliance.tis;

import com.example.pos.compliance.invoice.model.TaxInvoice;

import java.util.UUID;

public interface TraderInvoicingSystem {

    TaxInvoice generateInvoice(UUID saleId, String customerPin);

    String generateReceipt(UUID saleId, UUID invoiceId);

    void validateInvoice(TaxInvoice invoice);

    void submitToKra(TaxInvoice invoice);

    void cancelInvoice(UUID invoiceId, String reason);

    void issueCreditNote(UUID invoiceId, java.math.BigDecimal amount, String reason);

    void issueDebitNote(UUID invoiceId, java.math.BigDecimal amount, String reason);

    void reconciliationCheck();
}
