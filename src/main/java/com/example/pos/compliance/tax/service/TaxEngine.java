package com.example.pos.compliance.tax.service;

import com.example.pos.compliance.tax.dto.TaxSnapshot;
import com.example.pos.masterdata.tax.model.Tax;

import java.math.BigDecimal;

public interface TaxEngine {

    BigDecimal calculateTaxAmount(BigDecimal taxableAmount, Tax taxCategory);

    BigDecimal calculateTaxableAmount(BigDecimal unitPrice, int quantity, BigDecimal discount);

    Tax getApplicableTax(Long medicineId);

    TaxSnapshot snapshot(Tax tax);
}
