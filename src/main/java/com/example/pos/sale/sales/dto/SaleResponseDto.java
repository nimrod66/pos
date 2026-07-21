package com.example.pos.sale.sales.dto;

import com.example.pos.sale.sales.model.Sales;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleResponseDto {

    private Long id;
    private String invoiceNumber;
    private String saleStatus;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private Long branchId;
    private String branchName;
    private Long userId;
    private String userName;
    private Long customerId;
    private String customerName;
    private List<SaleItemResponse> items;
    private List<PaymentResponse> payments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SaleItemResponse {
        private Long id;
        private Long medicineBatchesId;
        private String batchNumber;
        private String medicineName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal discount;
        private BigDecimal taxRate;
        private BigDecimal taxableAmount;
        private BigDecimal tax;
        private BigDecimal total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResponse {
        private Long id;
        private String paymentMethod;
        private BigDecimal amount;
        private String currency;
        private String transactionReference;
        private String paymentStatus;
        private LocalDateTime paymentDate;
    }

    public static SaleResponseDto from(Sales sale) {
        return SaleResponseDto.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .saleStatus(sale.getSaleStatus() != null ? sale.getSaleStatus().name() : null)
                .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : null)
                .subtotal(sale.getSubtotal())
                .tax(sale.getTax())
                .total(sale.getTotal())
                .branchId(sale.getBranch() != null ? sale.getBranch().getId() : null)
                .branchName(sale.getBranch() != null ? sale.getBranch().getBranchName() : null)
                .userId(sale.getUser() != null ? sale.getUser().getId() : null)
                .userName(sale.getUser() != null ? sale.getUser().getFirstName() : null)
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .build();
    }
}
