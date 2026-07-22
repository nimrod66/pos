package com.example.pos.pharmacy.regulatory.controlledrugs.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlledDrugsRequestDto {

    @NotNull private Long medicineId;
    @NotNull private Long prescriptionId;
    @NotNull private Long userId;
    @NotNull private Integer quantityDispensed;
}
