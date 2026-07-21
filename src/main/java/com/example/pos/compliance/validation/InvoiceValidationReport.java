package com.example.pos.compliance.validation;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.model.TaxInvoiceItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoiceValidationReport {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String error) { errors.add(error); }
    public void addWarning(String warning) { warnings.add(warning); }

    public boolean isValid() { return errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public List<String> getErrors() { return List.copyOf(errors); }
    public List<String> getWarnings() { return List.copyOf(warnings); }
}
