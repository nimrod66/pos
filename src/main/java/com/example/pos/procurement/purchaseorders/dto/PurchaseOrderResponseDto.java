package com.example.pos.procurement.purchaseorders.dto;

import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
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
public class PurchaseOrderResponseDto {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long branchId;
    private String branchName;
    private Long orderedById;
    private String orderedByName;
    private Long approvedById;
    private String status;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime deliveryDate;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private Integer quantity;
        private BigDecimal buyingPrice;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal total;
    }

    public static PurchaseOrderResponseDto from(PurchaseOrders po) {
        return PurchaseOrderResponseDto.builder()
                .id(po.getId())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getSupplierName() : null)
                .branchId(po.getBranch() != null ? po.getBranch().getId() : null)
                .branchName(po.getBranch() != null ? po.getBranch().getBranchName() : null)
                .orderedById(po.getOrderedBy() != null ? po.getOrderedBy().getId() : null)
                .orderedByName(po.getOrderedBy() != null ? po.getOrderedBy().getFirstName() : null)
                .approvedById(po.getApprovedBy() != null ? po.getApprovedBy().getId() : null)
                .status(po.getStatus() != null ? po.getStatus().name() : null)
                .orderDate(po.getOrderDate()).expectedDeliveryDate(po.getExpectedDeliveryDate())
                .deliveryDate(po.getDeliveryDate())
                .createdAt(po.getCreatedAt()).updatedAt(po.getUpdatedAt()).build();
    }
}
