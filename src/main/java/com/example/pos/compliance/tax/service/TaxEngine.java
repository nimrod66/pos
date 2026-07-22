package com.example.pos.compliance.tax.service;

import com.example.pos.compliance.tax.dto.TaxSnapshot;

import java.math.BigDecimal;

public interface TaxEngine {

    BigDecimal calculateTaxAmount(BigDecimal taxableAmount, TaxSnapshot taxCategory);

    BigDecimal calculateTaxableAmount(BigDecimal unitPrice, int quantity, BigDecimal discount);

    TaxSnapshot snapshot(BigDecimal taxRate, String taxCode, String taxType);
}
