package com.example.pos.inventory.stockcount.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockCountRequestDto {

    @NotNull(message = "Count date is required")
    private LocalDate countDate;

    private String remarks;

    private List<StockCountItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockCountItemDto {
        @NotNull(message = "Batch ID is required")
        private UUID medicineBatchesId;

        @NotNull(message = "System quantity is required")
        private Integer systemQuantity;

        private Integer countedQuantity;
        private String remarks;
    }
}
