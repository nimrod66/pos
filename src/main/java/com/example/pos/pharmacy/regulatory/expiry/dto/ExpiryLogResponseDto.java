package com.example.pos.pharmacy.regulatory.expiry.dto;

import com.example.pos.pharmacy.regulatory.expiry.model.ExpiryLogs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiryLogResponseDto {

    private UUID id;
    private UUID medicineBatchesId;
    private String batchNumber;
    private String medicineName;
    private UUID userId;
    private String userName;
    private String disposalMethod;
    private LocalDateTime createdAt;

    public static ExpiryLogResponseDto from(ExpiryLogs log) {
        return ExpiryLogResponseDto.builder()
                .id(log.getId())
                .medicineBatchesId(log.getMedicineBatches() != null ? log.getMedicineBatches().getId() : null)
                .batchNumber(log.getMedicineBatches() != null ? log.getMedicineBatches().getBatchNumber() : null)
                .medicineName(log.getMedicineBatches() != null && log.getMedicineBatches().getMedicine() != null
                        ? log.getMedicineBatches().getMedicine().getBrandName() : null)
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getFirstName() : null)
                .disposalMethod(log.getDisposalMethod())
                .createdAt(log.getCreatedAt()).build();
    }
}