package com.example.pos.sale.salereturns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnRequestDto {

    @NotNull(message = "Client return ID is required")
    private UUID clientReturnId;

    @NotNull(message = "Sale ID is required")
    private UUID saleId;

    private UUID userId;

    @NotBlank(message = "Return reason is required")
    @Size(max = 500)
    private String reason;

    @NotBlank(message = "Refund method is required")
    private String refundMethod;

    @Size(max = 120)
    private String refundReference;

    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid ReturnItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDto {
        @NotNull private UUID saleItemId;
        private UUID medicineBatchesId;
        @NotNull @Positive private Integer quantity;
    }
}

