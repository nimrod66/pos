package com.example.pos.compliance.tax.dto;

import java.math.BigDecimal;

public record TaxSnapshot(
        BigDecimal taxRate,
        String taxCode,
        String taxType
) {}
