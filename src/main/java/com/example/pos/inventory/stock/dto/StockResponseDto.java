package com.example.pos.inventory.stock.dto;

import com.example.pos.inventory.stock.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponseDto {

    private UUID id;
    private UUID medicineBatchesId;
    private String batchNumber;
    private UUID medicineId;
    private String medicineName;
    private UUID branchId;
    private String branchName;
    private Integer quantityAvailable;
    private Integer reservedQuantity;
    private Integer minimumStock;
    private Integer maximumStock;
    private Integer reorderLevel;
    private String shelfLocation;
    private LocalDate lastStockDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StockResponseDto from(Stock stock) {
        MedicineBatchesInfo batchInfo = stock.getMedicineBatches() != null
                ? MedicineBatchesInfo.from(stock.getMedicineBatches())
                : new MedicineBatchesInfo();

        return StockResponseDto.builder()
                .id(stock.getId())
                .medicineBatchesId(stock.getMedicineBatches() != null ? stock.getMedicineBatches().getId() : null)
                .batchNumber(batchInfo.batchNumber)
                .medicineId(batchInfo.medicineId)
                .medicineName(batchInfo.medicineName)
                .branchId(stock.getBranch() != null ? stock.getBranch().getId() : null)
                .branchName(stock.getBranch() != null ? stock.getBranch().getBranchName() : null)
                .quantityAvailable(stock.getQuantityAvailable())
                .reservedQuantity(stock.getReservedQuantity())
                .minimumStock(stock.getMinimumStock())
                .maximumStock(stock.getMaximumStock())
                .reorderLevel(stock.getReorderLevel())
                .shelfLocation(stock.getShelfLocation())
                .lastStockDate(stock.getLastStockDate())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class MedicineBatchesInfo {
        UUID medicineId;
        String medicineName;
        String batchNumber;

        static MedicineBatchesInfo from(
                com.example.pos.inventory.batches.model.MedicineBatches batch) {
            MedicineBatchesInfo info = new MedicineBatchesInfo();
            info.batchNumber = batch.getBatchNumber();
            if (batch.getMedicine() != null) {
                info.medicineId = batch.getMedicine().getId();
                info.medicineName = batch.getMedicine().getBrandName();
            }
            return info;
        }
    }
}

