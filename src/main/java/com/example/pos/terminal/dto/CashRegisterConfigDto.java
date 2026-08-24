package com.example.pos.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterConfigDto {
    private BigDecimal defaultOpeningFloat;
    private boolean cashEnabled;
    private boolean mpesaEnabled;
    private boolean requireOpenShift;
    private boolean autoPrintReceipt;
    private boolean openDrawerOnCashSale;
    private int receiptCopies;
    private int receiptPaperWidth;
    private String scannerMode;
    private String barcodeSubmitKey;
}
