package com.example.pos.compliance.tax.service;

import com.example.pos.compliance.tax.dto.TaxSnapshot;
import com.example.pos.masterdata.tax.model.Tax;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
public class DefaultTaxEngine implements TaxEngine {

    @Override
    public BigDecimal calculateTaxAmount(BigDecimal taxableAmount, Tax taxCategory) {
        if (taxCategory == null || taxableAmount == null) return BigDecimal.ZERO;
        if (!taxCategory.isActive()) return BigDecimal.ZERO;

        BigDecimal rate = taxCategory.getTaxRate();
        return taxableAmount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTaxableAmount(BigDecimal unitPrice, int quantity, BigDecimal discount) {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discount == null) return subtotal;
        return subtotal.subtract(discount).max(BigDecimal.ZERO);
    }

    @Override
    public Tax getApplicableTax(Long medicineId) {
        return null;
    }

    @Override
    public TaxSnapshot snapshot(Tax tax) {
        if (tax == null) {
            return new TaxSnapshot(BigDecimal.ZERO, null, null);
        }
        return new TaxSnapshot(tax.getTaxRate(), tax.getCode(),
                tax.getTaxType() != null ? tax.getTaxType().name() : null);
    }
}
