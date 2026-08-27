package com.example.pos.inventory.stocktransfer.dto;

import com.example.pos.inventory.stocktransfer.model.StockTransfer;
import com.example.pos.inventory.stocktransfer.model.StockTransferItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferResponseDto {

    private UUID id;
    private UUID sourceBranchId;
    private String sourceBranchName;
    private UUID destBranchId;
    private String destBranchName;
    private String status;
    private UUID requestedById;
    private String requestedByName;
    private UUID approvedById;
    private String approvedByName;
    private UUID receivedById;
    private String receivedByName;
    private LocalDate transferDate;
    private LocalDate receivedDate;
    private String remarks;
    private List<StockTransferItemDto> items;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockTransferItemDto {
        private UUID id;
        private UUID medicineBatchesId;
        private String batchNumber;
        private String medicineName;
        private Integer quantity;
        private Integer receivedQuantity;
    }

    public static StockTransferResponseDto from(StockTransfer st) {
        return StockTransferResponseDto.builder()
                .id(st.getId())
                .sourceBranchId(st.getSourceBranch() != null ? st.getSourceBranch().getId() : null)
                .sourceBranchName(st.getSourceBranch() != null ? st.getSourceBranch().getBranchName() : null)
                .destBranchId(st.getDestBranch() != null ? st.getDestBranch().getId() : null)
                .destBranchName(st.getDestBranch() != null ? st.getDestBranch().getBranchName() : null)
                .status(st.getStatus())
                .requestedById(st.getRequestedBy() != null ? st.getRequestedBy().getId() : null)
                .requestedByName(st.getRequestedBy() != null
                        ? st.getRequestedBy().getFirstName() + " " + st.getRequestedBy().getLastName() : null)
                .approvedById(st.getApprovedBy() != null ? st.getApprovedBy().getId() : null)
                .approvedByName(st.getApprovedBy() != null
                        ? st.getApprovedBy().getFirstName() + " " + st.getApprovedBy().getLastName() : null)
                .receivedById(st.getReceivedBy() != null ? st.getReceivedBy().getId() : null)
                .receivedByName(st.getReceivedBy() != null
                        ? st.getReceivedBy().getFirstName() + " " + st.getReceivedBy().getLastName() : null)
                .transferDate(st.getTransferDate())
                .receivedDate(st.getReceivedDate())
                .remarks(st.getRemarks())
                .items(st.getItems() == null ? List.of() : st.getItems().stream()
                        .map(item -> StockTransferItemDto.builder()
                                .id(item.getId())
                                .medicineBatchesId(item.getMedicineBatches() != null ? item.getMedicineBatches().getId() : null)
                                .batchNumber(item.getMedicineBatches() != null ? item.getMedicineBatches().getBatchNumber() : null)
                                .medicineName(item.getMedicineBatches() != null && item.getMedicineBatches().getMedicine() != null
                                        ? item.getMedicineBatches().getMedicine().getBrandName() : null)
                                .quantity(item.getQuantity())
                                .receivedQuantity(item.getReceivedQuantity())
                                .build())
                        .toList())
                .createdAt(st.getCreatedAt())
                .build();
    }
}
