package com.example.pos.compliance.invoice.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "tax_invoices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoice_number", columnNames = "invoice_number")
})
public class TaxInvoice extends BaseEntity {

    @Column(name = "sale_id", unique = true)
    private UUID saleId;

    @Column(name = "branch_code", length = 30)
    private String branchCode;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvoiceStatus invoiceStatus = InvoiceStatus.DRAFT;

    @Column(name = "subtotal", precision = 15, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "discount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "KES";

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_pin", length = 20)
    private String customerPin;

    @Column(name = "schema_version")
    @Builder.Default
    private Integer schemaVersion = 1;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "qr_code_content", length = 2000)
    private String qrCodeContent;

    @Column(name = "qr_image_path", length = 500)
    private String qrImagePath;

    @Column(name = "verification_url", length = 1000)
    private String verificationUrl;

    @Builder.Default
    @OneToMany(mappedBy = "taxInvoice", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<TaxInvoiceItem> items = new ArrayList<>();
}
