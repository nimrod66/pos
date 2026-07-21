package com.example.pos.compliance.validation;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.model.TaxInvoiceItem;
import com.example.pos.compliance.tax.service.TaxEngine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ComplianceValidationService {

    private final TaxEngine taxEngine;

    public ComplianceValidationService(TaxEngine taxEngine) {
        this.taxEngine = taxEngine;
    }

    public InvoiceValidationReport validate(TaxInvoice invoice) {
        InvoiceValidationReport report = new InvoiceValidationReport();

        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            report.addError("Invoice number is required");
        }

        if (invoice.getSubtotal() == null || invoice.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
            report.addError("Subtotal must be greater than zero");
        }

        if (invoice.getGrandTotal() == null || invoice.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) {
            report.addError("Grand total must be greater than zero");
        }

        if (invoice.getCustomerPin() == null || invoice.getCustomerPin().isBlank()) {
            report.addWarning("Customer PIN is missing - may impact eTIMS compliance");
        }

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            report.addError("Invoice must have at least one item");
        } else {
            validateItems(invoice, report);
        }

        validateTotals(invoice, report);

        return report;
    }

    private void validateItems(TaxInvoice invoice, InvoiceValidationReport report) {
        for (int i = 0; i < invoice.getItems().size(); i++) {
            TaxInvoiceItem item = invoice.getItems().get(i);
            int lineNum = i + 1;

            if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                report.addError("Item " + lineNum + ": unit price must be greater than zero");
            }

            if (item.getTotal() != null && item.getTotal().compareTo(BigDecimal.ZERO) < 0) {
                report.addError("Item " + lineNum + ": total cannot be negative");
            }

            BigDecimal expectedSubtotal = item.getUnitPrice() != null
                    ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    : BigDecimal.ZERO;
            if (item.getSubtotal() != null && expectedSubtotal.compareTo(item.getSubtotal()) != 0) {
                report.addWarning("Item " + lineNum + ": subtotal mismatch (expected " + expectedSubtotal
                        + ", got " + item.getSubtotal() + ")");
            }
        }
    }

    private void validateTotals(TaxInvoice invoice, InvoiceValidationReport report) {
        if (invoice.getItems() == null) return;

        BigDecimal calculatedSubtotal = invoice.getItems().stream()
                .map(i -> i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal calculatedTax = invoice.getItems().stream()
                .map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal calculatedDiscount = invoice.getItems().stream()
                .map(i -> i.getDiscount() != null ? i.getDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal calculatedGrand = calculatedSubtotal.subtract(calculatedDiscount).add(calculatedTax);

        if (invoice.getSubtotal() != null && calculatedSubtotal.compareTo(invoice.getSubtotal()) != 0) {
            report.addWarning("Invoice subtotal mismatch: expected " + calculatedSubtotal
                    + ", got " + invoice.getSubtotal());
        }

        if (invoice.getGrandTotal() != null && calculatedGrand.compareTo(invoice.getGrandTotal()) != 0) {
            report.addWarning("Invoice grand total mismatch: expected " + calculatedGrand
                    + ", got " + invoice.getGrandTotal());
        }
    }
}
