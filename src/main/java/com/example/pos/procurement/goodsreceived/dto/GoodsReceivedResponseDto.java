package com.example.pos.procurement.goodsreceived.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceivedResponseDto {

    private UUID id;
    private UUID supplierId;
    private String supplierName;
    private String supplierInvoiceNumber;
    private UUID purchaseOrderId;
    private UUID branchId;
    private String branchName;
    private UUID receivedByUserId;
    private String receivedByName;
    private LocalDateTime receivedAt;
    private String remarks;
    private String idempotencyKey;
    private List<LineDto> lines;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineDto {
        private UUID id;
        private UUID medicineId;
        private String medicineName;
        private UUID batchId;
        private String batchNumber;
        private LocalDate expiryDate;
        private Integer quantity;
        private BigDecimal unitCost;
    }

    public static GoodsReceivedResponseDto from(
            com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes grn) {
        return GoodsReceivedResponseDto.builder()
                .id(grn.getId())
                .supplierId(grn.getSupplier() != null ? grn.getSupplier().getId() : null)
                .supplierName(grn.getSupplier() != null ? grn.getSupplier().getSupplierName() : null)
                .supplierInvoiceNumber(grn.getSupplierInvoiceNumber())
                .purchaseOrderId(grn.getPurchaseOrders() != null ? grn.getPurchaseOrders().getId() : null)
                .branchId(grn.getBranch() != null ? grn.getBranch().getId() : null)
                .branchName(grn.getBranch() != null ? grn.getBranch().getBranchName() : null)
                .receivedByUserId(grn.getReceivedBy() != null ? grn.getReceivedBy().getId() : null)
                .receivedByName(grn.getReceivedBy() != null
                        ? (grn.getReceivedBy().getFirstName() + " "
                        + (grn.getReceivedBy().getLastName() != null ? grn.getReceivedBy().getLastName() : "")).trim()
                        : null)
                .receivedAt(grn.getReceivedAt())
                .remarks(grn.getRemarks())
                .idempotencyKey(grn.getIdempotencyKey())
                .lines(grn.getLines() != null ? grn.getLines().stream()
                        .map(line -> LineDto.builder()
                                .id(line.getId())
                                .medicineId(line.getMedicine() != null ? line.getMedicine().getId() : null)
                                .medicineName(line.getMedicine() != null ? line.getMedicine().getBrandName() : null)
                                .batchId(line.getBatch() != null ? line.getBatch().getId() : null)
                                .batchNumber(line.getBatchNumber())
                                .expiryDate(line.getExpiryDate())
                                .quantity(line.getQuantity())
                                .unitCost(line.getUnitCost())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .createdAt(grn.getCreatedAt())
                .build();
    }
}
