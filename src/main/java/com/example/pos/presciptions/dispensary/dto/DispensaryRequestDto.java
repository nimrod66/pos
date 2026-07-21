package com.example.pos.presciptions.dispensary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispensaryRequestDto {

    @NotNull(message = "Medicine batch ID is required")
    private Long medicineBatchesId;

    @NotNull(message = "User/Pharmacist ID is required")
    private Long userId;

    @NotNull(message = "Prescription item ID is required")
    private Long prescriptionItemsId;

    @NotNull @Positive
    private Integer dispensedQuantity;
}
