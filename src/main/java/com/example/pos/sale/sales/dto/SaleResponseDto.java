package com.example.pos.sale.sales.dto;

import com.example.pos.sale.sales.model.Sales;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponseDto {

    private UUID id;
    private UUID saleId;
    private String invoiceNumber;
    private String saleNumber;
    private String status;
    private String saleStatus;
    private String paymentStatus;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal tax;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private BigDecimal refundTotal;
    private BigDecimal paidTotal;
    private BigDecimal cashTendered;
    private BigDecimal changeDue;
    private BigDecimal amountOwed;
    private UUID shiftId;
    private UUID prescriptionReferenceId;
    private UUID branchId;
    private String branchName;
    private UUID userId;
    private String userName;
    private UUID customerId;
    private String customerName;
    private String customerKraPin;
    private List<SaleItemResponse> items;
    private List<PaymentResponse> payments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private ReceiptResponse receipt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SaleItemResponse {
        private UUID id;
        private UUID lineId;
        private UUID medicineId;
        private UUID medicineBatchesId;
        private String batchNumber;
        private String medicineName;
        private Integer quantity;
        private Integer returnedQuantity;
        private BigDecimal price;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal taxRate;
        private BigDecimal taxableAmount;
        private BigDecimal tax;
        private BigDecimal total;
        private BigDecimal lineTotal;
        private List<BatchAllocationResponse> allocations;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchAllocationResponse {
        private UUID saleItemId;
        private UUID batchId;
        private String batchNumber;
        private Integer quantity;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResponse {
        private UUID id;
        private String paymentMethod;
        private BigDecimal amount;
        private String currency;
        private String transactionReference;
        private String merchantRequestId;
        private String checkoutRequestId;
        private String paymentStatus;
        private LocalDateTime paymentDate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReceiptResponse {
        private String receiptNumber;
        private boolean printable;
    }

    public static SaleResponseDto from(Sales sale) {
        return SaleResponseDto.builder()
                .id(sale.getId())
                .saleId(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .saleNumber(sale.getInvoiceNumber())
                .status(sale.getSaleStatus() != null ? sale.getSaleStatus().name() : null)
                .saleStatus(sale.getSaleStatus() != null ? sale.getSaleStatus().name() : null)
                .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : null)
                .currency(sale.getCurrency())
                .subtotal(sale.getSubtotal())
                .discountTotal(sale.getDiscountTotal())
                .tax(sale.getTax())
                .taxTotal(sale.getTax())
                .total(sale.getTotal())
                .paidTotal(sale.getPaidTotal())
                .cashTendered(sale.getCashTendered())
                .changeDue(sale.getChangeDue())
                .amountOwed(sale.getAmountOwed())
                .shiftId(sale.getShift() != null ? sale.getShift().getId() : null)
                .branchId(sale.getBranch() != null ? sale.getBranch().getId() : null)
                .branchName(sale.getBranch() != null ? sale.getBranch().getBranchName() : null)
                .userId(sale.getUser() != null ? sale.getUser().getId() : null)
                .userName(sale.getUser() != null ? sale.getUser().getFirstName() : null)
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .completedAt(sale.getCompletedAt())
                .receipt(sale.getReceipts() != null ? sale.getReceipts().stream().findFirst()
                        .map(receipt -> ReceiptResponse.builder()
                                .receiptNumber(receipt.getReceiptNumber())
                                .printable(true)
                                .build())
                        .orElse(null) : null)
                .build();
    }
}
