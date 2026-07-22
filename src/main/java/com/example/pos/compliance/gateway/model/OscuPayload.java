package com.example.pos.compliance.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OscuPayload {
    private String invoiceNumber;
    private String invoiceDate;
    private String currency;
    private String customerPin;
    private String customerName;
    private String supplierPin;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private List<OscuPayloadItem> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OscuPayloadItem {
        private String itemCode;
        private String itemBarcodeType;
        private String itemClassificationCode;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxableAmount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal total;
    }
}
