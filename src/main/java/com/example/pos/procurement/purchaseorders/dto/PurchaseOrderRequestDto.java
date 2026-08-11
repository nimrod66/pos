package com.example.pos.procurement.purchaseorders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequestDto {

    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    @NotNull(message = "User ID is required")
    private UUID orderedById;

    private LocalDateTime expectedDeliveryDate;

    @NotEmpty
    @Valid
    private List<OrderItemDto> items;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemDto {
        @NotNull private UUID medicineId;
        @NotNull @Positive private Integer quantity;
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
        private BigDecimal buyingPrice;
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
        private BigDecimal discount;
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
        private BigDecimal tax;
    }
}

