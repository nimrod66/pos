package com.example.pos.sale.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDto {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String invoiceNumber;
    private String idempotencyKey;
    private Long customerId;

    @NotNull(message = "Items are required")
    private List<SaleItemDto> items;

    private List<PaymentItemDto> payments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemDto {
        @NotNull private Long medicineBatchesId;
        @NotNull @Positive private Integer quantity;
        @NotNull @Positive private BigDecimal price;
        private BigDecimal discount;
        private BigDecimal tax;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItemDto {
        @NotBlank private String paymentMethod;
        @NotNull @Positive private BigDecimal amount;
        private String currency;
        private String transactionReference;
    }
}
