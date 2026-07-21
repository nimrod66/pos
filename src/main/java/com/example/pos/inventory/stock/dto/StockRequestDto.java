package com.example.pos.inventory.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockRequestDto {

    @NotNull(message = "Medicine batch ID is required")
    private Long medicineBatchesId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @Min(value = 0, message = "Quantity available must be >= 0")
    private Integer quantityAvailable;

    @Min(value = 0, message = "Reserved quantity must be >= 0")
    private Integer reservedQuantity;

    @Min(value = 0, message = "Minimum stock must be >= 0")
    private Integer minimumStock;

    @Min(value = 0, message = "Maximum stock must be >= 0")
    private Integer maximumStock;

    @Min(value = 0, message = "Reorder level must be >= 0")
    private Integer reorderLevel;

    private String shelfLocation;
    private LocalDate lastStockDate;
}
