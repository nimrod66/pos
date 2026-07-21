package com.example.pos.inventory.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Medicine batch ID is required")
    private Long medicineBatchesId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}
