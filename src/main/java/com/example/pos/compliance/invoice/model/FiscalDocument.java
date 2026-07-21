package com.example.pos.compliance.invoice.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class FiscalDocument extends BaseEntity {

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "document_status", nullable = false, length = 20)
    private String documentStatus;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "schema_version")
    private Integer schemaVersion;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "discount", precision = 15, scale = 2)
    private BigDecimal discount;

    @Column(name = "grand_total", precision = 15, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "qr_code_content", length = 2000)
    private String qrCodeContent;

    @Column(name = "qr_image_path", length = 500)
    private String qrImagePath;

    @Column(name = "verification_url", length = 1000)
    private String verificationUrl;
}
