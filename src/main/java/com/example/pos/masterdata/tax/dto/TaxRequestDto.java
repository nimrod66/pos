package com.example.pos.masterdata.tax.dto;

import com.example.pos.masterdata.tax.model.TaxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxRequestDto {

    @NotBlank(message = "Tax code is required")
    private String code;

    @NotBlank(message = "Tax name is required")
    private String taxName;

    private String taxDescription;

    @NotNull(message = "Tax rate is required")
    @PositiveOrZero(message = "Tax rate must be zero or positive")
    private BigDecimal taxRate;

    @NotNull(message = "Tax type is required")
    private TaxType taxType;

    private boolean active = true;
}
