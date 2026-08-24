package com.example.pos.terminal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterConfigRequestDto {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal defaultOpeningFloat;
    @NotNull private Boolean cashEnabled;
    @NotNull private Boolean mpesaEnabled;
    @NotNull private Boolean requireOpenShift;
    @NotNull private Boolean autoPrintReceipt;
    @NotNull private Boolean openDrawerOnCashSale;
    @NotNull @Min(1) @Max(3) private Integer receiptCopies;
    @NotNull @Min(58) @Max(80) private Integer receiptPaperWidth;
    @NotBlank private String scannerMode;
    @NotBlank private String barcodeSubmitKey;
}
