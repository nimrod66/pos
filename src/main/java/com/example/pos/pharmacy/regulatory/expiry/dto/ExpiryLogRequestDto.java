package com.example.pos.pharmacy.regulatory.expiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryLogRequestDto {

    @NotNull private UUID medicineBatchesId;
    @NotNull private UUID userId;
    @NotBlank private String disposalMethod;
}