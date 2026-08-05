package com.example.pos.masterdata.units.dto;

import com.example.pos.masterdata.units.model.Unit;
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
public class UnitResponseDto {

    private UUID id;
    private String unitName;
    private String unitAbbreviation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UnitResponseDto from(Unit unit) {
        return UnitResponseDto.builder()
                .id(unit.getId())
                .unitName(unit.getUnitName())
                .unitAbbreviation(unit.getUnitAbbreviation())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .build();
    }
}

