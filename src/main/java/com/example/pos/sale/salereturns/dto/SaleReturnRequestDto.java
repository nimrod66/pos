package com.example.pos.sale.salereturns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnRequestDto {

    @NotNull(message = "Sale ID is required")
    private UUID saleId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Return reason is required")
    private String reason;

    @NotNull
    private List<ReturnItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDto {
        @NotNull private UUID saleItemId;
        @NotNull private UUID medicineBatchesId;
        @NotNull @Positive private Integer quantity;
    }
}

