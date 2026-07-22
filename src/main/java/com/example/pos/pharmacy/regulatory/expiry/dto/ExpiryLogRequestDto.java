package com.example.pos.pharmacy.regulatory.expiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryLogRequestDto {

    @NotNull private Long medicineBatchesId;
    @NotNull private Long userId;
    @NotBlank private String disposalMethod;
}
