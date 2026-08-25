package com.example.pos.pharmacy.regulatory.expiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryLogRequestDto {

    @NotNull private UUID medicineBatchesId;
    @NotBlank private String disposalMethod;

    @NotNull
    @Min(value = 1, message = "Dispose at least one unit")
    private Integer quantityDisposed;
}
