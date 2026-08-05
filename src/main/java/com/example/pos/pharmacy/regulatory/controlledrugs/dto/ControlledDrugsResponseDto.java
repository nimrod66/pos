package com.example.pos.pharmacy.regulatory.controlledrugs.dto;

import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
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
public class ControlledDrugsResponseDto {

    private UUID id;
    private UUID medicineId;
    private String medicineName;
    private UUID prescriptionId;
    private UUID userId;
    private String userName;
    private Integer quantityDispensed;
    private LocalDateTime createdAt;

    public static ControlledDrugsResponseDto from(ControlledDrugs cd) {
        return ControlledDrugsResponseDto.builder()
                .id(cd.getId()).medicineId(cd.getMedicine() != null ? cd.getMedicine().getId() : null)
                .medicineName(cd.getMedicine() != null ? cd.getMedicine().getBrandName() : null)
                .prescriptionId(cd.getPrescriptions() != null ? cd.getPrescriptions().getId() : null)
                .userId(cd.getUser() != null ? cd.getUser().getId() : null)
                .userName(cd.getUser() != null ? cd.getUser().getFirstName() : null)
                .quantityDispensed(cd.getQuantityDispensed())
                .createdAt(cd.getCreatedAt()).build();
    }
}