package com.example.pos.inventory.stockmovements.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequestDto {

    @NotBlank(message = "Movement type is required")
    private String movementType;

    @NotNull(message = "Medicine batch ID is required")
    private Long medicineBatchesId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private String referenceType;
    private Long referenceId;
    private LocalDate movementDate;
}
