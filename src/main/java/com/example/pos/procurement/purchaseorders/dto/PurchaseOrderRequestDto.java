package com.example.pos.procurement.purchaseorders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequestDto {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "User ID is required")
    private Long orderedById;

    private LocalDateTime expectedDeliveryDate;

    @NotNull
    private List<OrderItemDto> items;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemDto {
        @NotNull private Long medicineId;
        @NotNull @Positive private Integer quantity;
        @NotNull @Positive private BigDecimal buyingPrice;
        private BigDecimal discount;
        private BigDecimal tax;
    }
}
