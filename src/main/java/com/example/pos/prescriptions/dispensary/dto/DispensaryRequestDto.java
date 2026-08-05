package com.example.pos.prescriptions.dispensary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispensaryRequestDto {

    @NotNull(message = "Medicine batch ID is required")
    private UUID medicineBatchesId;

    @NotNull(message = "User/Pharmacist ID is required")
    private UUID userId;

    @NotNull(message = "Prescription item ID is required")
    private UUID prescriptionItemsId;

    @NotNull @Positive
    private Integer dispensedQuantity;
}

