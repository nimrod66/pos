package com.example.pos.inventory.batches.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineBatchRequestDto {

    @NotNull(message = "Medicine ID is required")
    private UUID medicineId;

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    private LocalDate manufactureDate;

    private LocalDate expirationDate;

    @NotNull(message = "Initial quantity is required")
    @Positive(message = "Initial quantity must be positive")
    private Integer initialQuantity;

    @NotNull(message = "Buying price is required")
    @Positive(message = "Buying price must be positive")
    private BigDecimal buyingPrice;

    @NotNull(message = "Selling price is required")
    @Positive(message = "Selling price must be positive")
    private BigDecimal sellingPrice;
}

