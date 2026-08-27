package com.example.pos.inventory.stocktransfer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferRequestDto {

    @NotNull(message = "Source branch ID is required")
    private UUID sourceBranchId;

    @NotNull(message = "Destination branch ID is required")
    private UUID destBranchId;

    private String remarks;

    private List<StockTransferItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockTransferItemDto {
        @NotNull(message = "Batch ID is required")
        private UUID medicineBatchesId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}
