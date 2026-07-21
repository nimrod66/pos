package com.example.pos.sale.receipts.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptData {
    private String storeName;
    private String branchName;
    private String address;
    private String phone;
    private String invoiceNumber;
    private LocalDateTime dateTime;
    private String cashier;
    private String currency;

    private List<ReceiptItem> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;

    private String paymentMethods;
    private String footerText;
    private String thankYou;
    private String returnPolicy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptItem {
        private int number;
        private String name;
        private String batchNumber;
        private int qty;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal total;
    }
}
