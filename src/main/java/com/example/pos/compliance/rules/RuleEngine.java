package com.example.pos.compliance.rules;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.validation.InvoiceValidationReport;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class RuleEngine {

    private final List<ComplianceRule> rules;

    public RuleEngine(List<ComplianceRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(ComplianceRule::getPriority))
                .toList();
    }

    public InvoiceValidationReport evaluate(TaxInvoice invoice) {
        InvoiceValidationReport report = new InvoiceValidationReport();
        for (ComplianceRule rule : rules) {
            try {
                rule.evaluate(invoice, report);
            } catch (Exception e) {
                report.addError("Rule " + rule.getName() + " failed: " + e.getMessage());
            }
        }
        return report;
    }
}
