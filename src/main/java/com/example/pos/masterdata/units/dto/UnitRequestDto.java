package com.example.pos.masterdata.units.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitRequestDto {

    @NotBlank(message = "Unit name is required")
    private String unitName;

    private String unitAbbreviation;

    private UUID parentUnitId;

    private Integer conversionFactor;
}
