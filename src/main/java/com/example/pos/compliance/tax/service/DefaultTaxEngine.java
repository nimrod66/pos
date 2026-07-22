package com.example.pos.compliance.tax.service;

import com.example.pos.compliance.tax.dto.TaxSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DefaultTaxEngine implements TaxEngine {

    @Override
    public BigDecimal calculateTaxAmount(BigDecimal taxableAmount, TaxSnapshot taxCategory) {
        if (taxCategory == null || taxableAmount == null) return BigDecimal.ZERO;

        BigDecimal rate = taxCategory.taxRate();
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
    public TaxSnapshot snapshot(BigDecimal taxRate, String taxCode, String taxType) {
        return new TaxSnapshot(
                taxRate != null ? taxRate : BigDecimal.ZERO,
                taxCode,
                taxType);
    }
}
