package com.example.pos.compliance.invoice.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "tax_invoice_items")
public class TaxInvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_invoice_id", nullable = false)
    private TaxInvoice taxInvoice;

    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(name = "medicine_name")
    private String medicineName;

    private String barcode;

    @Column(name = "barcode_type", length = 20)
    private String barcodeType;

    @Column(name = "etims_classification_code", length = 50)
    private String etimsClassificationCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "taxable_amount", precision = 15, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "tax_rate", precision = 10, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "tax_type")
    private String taxType;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 15, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "total", precision = 15, scale = 2, nullable = false)
    private BigDecimal total;
}
