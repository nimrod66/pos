package com.example.pos.compliance.invoice.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleFiscalData(
        Long saleId,
        boolean cancelled,
        Long branchId,
        String branchCode,
        BigDecimal subtotal,
        Long customerId,
        String customerName,
        String customerPin,
        String currency,
        List<SaleFiscalItemData> items
) {}