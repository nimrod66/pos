package com.example.pos.compliance.invoice.dto;

import com.example.pos.compliance.invoice.model.TaxInvoice;
import com.example.pos.compliance.invoice.model.InvoiceHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxInvoiceResponseDto {

    private Long id;
    private Long saleId;
    private String invoiceNumber;
    private String invoiceStatus;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private LocalDateTime issueDate;
    private String currency;
    private Long branchId;
    private Long customerId;
    private String customerName;
    private String customerPin;
    private Integer schemaVersion;
    private String qrCodeContent;
    private String qrImagePath;
    private String verificationUrl;
    private List<ItemResponse> items;
    private List<HistoryResponse> history;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private String barcode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxableAmount;
        private BigDecimal taxRate;
        private String taxType;
        private BigDecimal taxAmount;
        private BigDecimal discount;
        private BigDecimal subtotal;
        private BigDecimal total;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HistoryResponse {
        private Long id;
        private String historyType;
        private String description;
        private Long actorId;
        private String actorName;
        private LocalDateTime createdAt;
    }

    public static TaxInvoiceResponseDto from(TaxInvoice invoice) {
        var builder = TaxInvoiceResponseDto.builder()
                .id(invoice.getId())
                .saleId(invoice.getSaleId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceStatus(invoice.getInvoiceStatus() != null ? invoice.getInvoiceStatus().name() : null)
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .discount(invoice.getDiscount())
                .grandTotal(invoice.getGrandTotal())
                .issueDate(invoice.getIssueDate())
                .currency(invoice.getCurrency())
                .branchId(invoice.getBranchId())
                .customerId(invoice.getCustomerId())
                .customerName(invoice.getCustomerName())
                .customerPin(invoice.getCustomerPin())
                .schemaVersion(invoice.getSchemaVersion())
                .qrCodeContent(invoice.getQrCodeContent())
                .qrImagePath(invoice.getQrImagePath())
                .verificationUrl(invoice.getVerificationUrl())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt());

        if (invoice.getItems() != null) {
            builder.items(invoice.getItems().stream().map(item -> ItemResponse.builder()
                    .id(item.getId())
                    .medicineId(item.getMedicineId())
                    .medicineName(item.getMedicineName())
                    .barcode(item.getBarcode())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .taxableAmount(item.getTaxableAmount())
                    .taxRate(item.getTaxRate())
                    .taxType(item.getTaxType())
                    .taxAmount(item.getTaxAmount())
                    .discount(item.getDiscount())
                    .subtotal(item.getSubtotal())
                    .total(item.getTotal())
                    .build()).toList());
        }

        return builder.build();
    }

    public static TaxInvoiceResponseDto from(TaxInvoice invoice, List<InvoiceHistory> histories) {
        var dto = from(invoice);
        if (histories != null) {
            dto.setHistory(histories.stream().map(h -> HistoryResponse.builder()
                    .id(h.getId())
                    .historyType(h.getHistoryType() != null ? h.getHistoryType().name() : null)
                    .description(h.getDescription())
                    .actorId(h.getActorId())
                    .actorName(h.getActorName())
                    .createdAt(h.getCreatedAt())
                    .build()).toList());
        }
        return dto;
    }
}
