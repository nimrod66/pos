package com.example.pos.sale.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDto {

    @NotNull
    private UUID clientSaleId;

    @NotNull
    private UUID shiftId;

    private UUID prescriptionReferenceId;
    private UUID customerId;

    @NotEmpty
    private List<@Valid SaleItemDto> items;

    @NotEmpty
    private List<@Valid PaymentItemDto> payments;

    @DecimalMin(value = "0.00")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal cashTendered;

    @Size(max = 500)
    private String note;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemDto {
        @NotNull
        private UUID lineId;

        @NotNull
        private UUID medicineId;

        private UUID sellingUnitId;

        @NotNull
        @DecimalMin(value = "1", inclusive = true)
        private BigDecimal quantity;

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 12, fraction = 2)
        private BigDecimal expectedUnitPrice;

        private UUID requestedBatchId;
        private UUID discountRequestId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItemDto {
        @NotBlank
        private String method;

        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 12, fraction = 2)
        private BigDecimal amount;

        @Size(max = 100)
        private String reference;
    }
}
