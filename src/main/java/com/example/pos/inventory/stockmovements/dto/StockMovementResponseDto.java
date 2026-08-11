package com.example.pos.inventory.stockmovements.dto;

import com.example.pos.inventory.stockmovements.model.StockMovements;
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
public class StockMovementResponseDto {

    private UUID id;
    private String movementType;
    private UUID medicineBatchesId;
    private String batchNumber;
    private UUID medicineId;
    private String medicineName;
    private UUID userId;
    private String userName;
    private UUID branchId;
    private String branchName;
    private String referenceType;
    private UUID referenceId;
    private LocalDate movementDate;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StockMovementResponseDto from(StockMovements movement) {
        return StockMovementResponseDto.builder()
                .id(movement.getId())
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .medicineBatchesId(movement.getMedicineBatches() != null ? movement.getMedicineBatches().getId() : null)
                .batchNumber(movement.getMedicineBatches() != null ? movement.getMedicineBatches().getBatchNumber() : null)
                .medicineId(movement.getMedicineBatches() != null && movement.getMedicineBatches().getMedicine() != null
                        ? movement.getMedicineBatches().getMedicine().getId() : null)
                .medicineName(movement.getMedicineBatches() != null && movement.getMedicineBatches().getMedicine() != null
                        ? movement.getMedicineBatches().getMedicine().getBrandName() : null)
                .userId(movement.getUser() != null ? movement.getUser().getId() : null)
                .userName(movement.getUser() != null
                        ? movement.getUser().getFirstName() + " " + movement.getUser().getLastName() : null)
                .branchId(movement.getBranch() != null ? movement.getBranch().getId() : null)
                .branchName(movement.getBranch() != null ? movement.getBranch().getBranchName() : null)
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .movementDate(movement.getMovementDate())
                .quantity(movement.getQuantity())
                .createdAt(movement.getCreatedAt())
                .updatedAt(movement.getUpdatedAt())
                .build();
    }
}

