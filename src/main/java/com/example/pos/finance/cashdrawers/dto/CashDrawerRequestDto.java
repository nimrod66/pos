package com.example.pos.finance.cashdrawers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashDrawerRequestDto {

    @NotNull(message = "Staff shift ID is required")
    private Long staffShiftsId;

    private BigDecimal openingBalance;
    private BigDecimal expectedClosingBalance;
    private BigDecimal actualClosingBalance;
    private BigDecimal variance;
}
