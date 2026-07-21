package com.example.pos.compliance.tis;

import com.example.pos.compliance.invoice.model.TaxInvoice;

public interface TraderInvoicingSystem {

    TaxInvoice generateInvoice(Long saleId, String customerPin);

    String generateReceipt(Long saleId, Long invoiceId);

    void validateInvoice(TaxInvoice invoice);

    void submitToKra(TaxInvoice invoice);

    void cancelInvoice(Long invoiceId, String reason);

    void issueCreditNote(Long invoiceId, java.math.BigDecimal amount, String reason);

    void issueDebitNote(Long invoiceId, java.math.BigDecimal amount, String reason);

    void reconciliationCheck();
}
