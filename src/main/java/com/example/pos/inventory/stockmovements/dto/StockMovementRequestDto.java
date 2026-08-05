package com.example.pos.inventory.stockmovements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequestDto {

    @NotBlank(message = "Movement type is required")
    private String movementType;

    @NotNull(message = "Medicine batch ID is required")
    private UUID medicineBatchesId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    private String referenceType;
    private UUID referenceId;
    private LocalDate movementDate;
}

