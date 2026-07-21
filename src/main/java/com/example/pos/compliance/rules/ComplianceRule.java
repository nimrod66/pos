package com.example.pos.compliance.rules;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.validation.InvoiceValidationReport;

public interface ComplianceRule {

    String getName();

    int getPriority();

    void evaluate(TaxInvoice invoice, InvoiceValidationReport report);
}
