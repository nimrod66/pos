package com.example.pos.inventory.stockcount.dto;

import com.example.pos.inventory.stockcount.model.StockCount;
import com.example.pos.inventory.stockcount.model.StockCountItem;
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
public class StockCountResponseDto {

    private UUID id;
    private UUID branchId;
    private String branchName;
    private LocalDate countDate;
    private String status;
    private UUID countedById;
    private String countedByName;
    private UUID reviewedById;
    private String reviewedByName;
    private String remarks;
    private List<StockCountItemDto> items;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockCountItemDto {
        private UUID id;
        private UUID medicineBatchesId;
        private String batchNumber;
        private String medicineName;
        private Integer systemQuantity;
        private Integer countedQuantity;
        private Integer variance;
        private String remarks;
    }

    public static StockCountResponseDto from(StockCount sc) {
        return StockCountResponseDto.builder()
                .id(sc.getId())
                .branchId(sc.getBranch() != null ? sc.getBranch().getId() : null)
                .branchName(sc.getBranch() != null ? sc.getBranch().getBranchName() : null)
                .countDate(sc.getCountDate())
                .status(sc.getStatus())
                .countedById(sc.getCountedBy() != null ? sc.getCountedBy().getId() : null)
                .countedByName(sc.getCountedBy() != null
                        ? sc.getCountedBy().getFirstName() + " " + sc.getCountedBy().getLastName() : null)
                .reviewedById(sc.getReviewedBy() != null ? sc.getReviewedBy().getId() : null)
                .reviewedByName(sc.getReviewedBy() != null
                        ? sc.getReviewedBy().getFirstName() + " " + sc.getReviewedBy().getLastName() : null)
                .remarks(sc.getRemarks())
                .items(sc.getItems() == null ? List.of() : sc.getItems().stream()
                        .map(item -> StockCountItemDto.builder()
                                .id(item.getId())
                                .medicineBatchesId(item.getMedicineBatches() != null ? item.getMedicineBatches().getId() : null)
                                .batchNumber(item.getMedicineBatches() != null ? item.getMedicineBatches().getBatchNumber() : null)
                                .medicineName(item.getMedicineBatches() != null && item.getMedicineBatches().getMedicine() != null
                                        ? item.getMedicineBatches().getMedicine().getBrandName() : null)
                                .systemQuantity(item.getSystemQuantity())
                                .countedQuantity(item.getCountedQuantity())
                                .variance(item.getVariance())
                                .remarks(item.getRemarks())
                                .build())
                        .toList())
                .createdAt(sc.getCreatedAt())
                .build();
    }
}
