package com.example.pos.inventory.batches.dto;

import com.example.pos.inventory.batches.model.MedicineBatches;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineBatchResponseDto {

    private UUID id;
    private UUID medicineId;
    private String medicineName;
    private String batchNumber;
    private LocalDate manufactureDate;
    private LocalDate expirationDate;
    private Integer initialQuantity;
    private BigDecimal buyingPrice;
    private BigDecimal sellingPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MedicineBatchResponseDto from(MedicineBatches batch) {
        return MedicineBatchResponseDto.builder()
                .id(batch.getId())
                .medicineId(batch.getMedicine() != null ? batch.getMedicine().getId() : null)
                .medicineName(batch.getMedicine() != null ? batch.getMedicine().getBrandName() : null)
                .batchNumber(batch.getBatchNumber())
                .manufactureDate(batch.getManufactureDate())
                .expirationDate(batch.getExpirationDate())
                .initialQuantity(batch.getInitialQuantity())
                .buyingPrice(batch.getBuyingPrice())
                .sellingPrice(batch.getSellingPrice())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}

