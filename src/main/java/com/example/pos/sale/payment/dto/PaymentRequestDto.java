package com.example.pos.sale.payment.dto;

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
public class PaymentRequestDto {

    @NotNull(message = "Sale ID is required")
    private UUID saleId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotNull @Positive
    private BigDecimal amount;

    private String currency;
    private String transactionReference;
    private String description;
    private String phoneNumber;
}

