package com.example.pos.procurement.supplierinvoices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoiceRequestDto {

    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotNull @Positive
    private BigDecimal subTotal;
    private BigDecimal tax;

    @NotNull @Positive
    private BigDecimal total;
}

