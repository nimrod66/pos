package com.example.pos.user.staffshifts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShiftStatusDto {

    private String status;

    @DecimalMin(value = "0.00")
    private java.math.BigDecimal actualCash;

    private String remarks;
}
