package com.example.pos.compliance.receipt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDTO {

    private String receiptNumber;
    private String invoiceNumber;
    private LocalDateTime issueDate;
    private String currency;

    private String businessName;
    private String branchName;
    private String address;
    private String phone;
    private String logoBase64;

    private String kraPin;
    private String qrCodeContent;
    private String verificationUrl;

    private String customerName;
    private String customerPin;

    private String cashierName;
    private String cashierId;
    private String terminalId;
    private String terminalName;
    private String terminalType;

    private List<ReceiptLine> items;
    private List<PaymentLine> payments;

    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;

    private String footerText;
    private String returnPolicy;
    private String thankYouMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptLine {
        private int lineNumber;
        private String name;
        private String barcode;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal taxableAmount;
        private BigDecimal taxRate;
        private String taxCode;
        private BigDecimal taxAmount;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentLine {
        private String method;
        private BigDecimal amount;
        private String reference;
        private BigDecimal changeGiven;
    }
}
