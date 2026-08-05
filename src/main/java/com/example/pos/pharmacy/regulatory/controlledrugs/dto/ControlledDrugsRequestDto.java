package com.example.pos.pharmacy.regulatory.controlledrugs.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlledDrugsRequestDto {

    @NotNull private UUID medicineId;
    @NotNull private UUID prescriptionId;
    @NotNull private UUID userId;
    @NotNull private Integer quantityDispensed;
}