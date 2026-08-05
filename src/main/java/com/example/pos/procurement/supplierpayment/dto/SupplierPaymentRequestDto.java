package com.example.pos.procurement.supplierpayment.dto;

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
public class SupplierPaymentRequestDto {

    @NotNull(message = "Supplier invoice ID is required")
    private UUID supplierInvoiceId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull @Positive
    private BigDecimal paymentAmount;

    private String paymentReference;
}

