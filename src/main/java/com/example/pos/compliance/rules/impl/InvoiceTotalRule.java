package com.example.pos.compliance.rules.impl;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.rules.ComplianceRule;
import com.example.pos.compliance.validation.InvoiceValidationReport;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(20)
public class InvoiceTotalRule implements ComplianceRule {

    @Override
    public String getName() {
        return "InvoiceTotal";
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public void evaluate(TaxInvoice invoice, InvoiceValidationReport report) {
        if (invoice.getSubtotal() == null || invoice.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
            report.addError("Invoice subtotal must be greater than zero");
        }
        if (invoice.getGrandTotal() == null || invoice.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) {
            report.addError("Invoice grand total must be greater than zero");
        }
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            report.addError("Invoice must have at least one line item");
        }
    }
}
