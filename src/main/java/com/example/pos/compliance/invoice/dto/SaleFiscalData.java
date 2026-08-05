package com.example.pos.compliance.invoice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleFiscalData(
        UUID saleId,
        boolean cancelled,
        UUID branchId,
        String branchCode,
        BigDecimal subtotal,
        UUID customerId,
        String customerName,
        String customerPin,
        String currency,
        List<SaleFiscalItemData> items
) {}