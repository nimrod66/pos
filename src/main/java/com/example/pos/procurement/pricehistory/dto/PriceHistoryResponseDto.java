package com.example.pos.procurement.pricehistory.dto;

import com.example.pos.procurement.pricehistory.model.PriceHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistoryResponseDto {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private Long medicineBatchId;
    private String batchNumber;
    private Long userId;
    private String userName;
    private BigDecimal oldBuyingPrice;
    private BigDecimal oldSellingPrice;
    private BigDecimal newBuyingPrice;
    private BigDecimal newSellingPrice;
    private LocalDateTime changedAt;
    private LocalDateTime createdAt;

    public static PriceHistoryResponseDto from(PriceHistory ph) {
        return PriceHistoryResponseDto.builder()
                .id(ph.getId())
                .medicineId(ph.getMedicine() != null ? ph.getMedicine().getId() : null)
                .medicineName(ph.getMedicine() != null ? ph.getMedicine().getBrandName() : null)
                .medicineBatchId(ph.getMedicineBatches() != null ? ph.getMedicineBatches().getId() : null)
                .batchNumber(ph.getMedicineBatches() != null ? ph.getMedicineBatches().getBatchNumber() : null)
                .userId(ph.getUser() != null ? ph.getUser().getId() : null)
                .userName(ph.getUser() != null ? ph.getUser().getFirstName() + " " + ph.getUser().getLastName() : null)
                .oldBuyingPrice(ph.getOldBuyingPrice())
                .oldSellingPrice(ph.getOldSellingPrice())
                .newBuyingPrice(ph.getNewBuyingPrice())
                .newSellingPrice(ph.getNewSellingPrice())
                .changedAt(ph.getChangedAt())
                .createdAt(ph.getCreatedAt())
                .build();
    }
}
