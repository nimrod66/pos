package com.example.pos.procurement.goodsreceived.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceivedRequestDto {

    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    private String supplierInvoiceNumber;

    private UUID purchaseOrdersId;

    private LocalDateTime receivedAt;

    private String remarks;

    @NotEmpty(message = "At least one received line is required")
    @Valid
    private List<GRNLineDto> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GRNLineDto {
        @NotNull
        private UUID medicineId;

        private UUID purchaseOrderLineId;

        @NotBlank
        private String batchNumber;

        private LocalDate expiryDate;

        @NotNull
        @Min(1)
        private Integer quantity;

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        private BigDecimal unitCost;
    }
}
