package com.example.pos.compliance.rules.impl;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.rules.ComplianceRule;
import com.example.pos.compliance.invoice.validation.InvoiceValidationReport;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class RequiredCustomerPinRule implements ComplianceRule {

    @Override
    public String getName() {
        return "RequiredCustomerPin";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public void evaluate(TaxInvoice invoice, InvoiceValidationReport report) {
        if (invoice.getCustomerPin() == null || invoice.getCustomerPin().isBlank()) {
            report.addWarning("Customer PIN is missing — required for eTIMS compliance");
        }
    }
}
