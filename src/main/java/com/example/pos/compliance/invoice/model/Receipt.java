package com.example.pos.compliance.invoice.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "receipts_compliance")
public class Receipt extends BaseEntity {

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "receipt_number", unique = true, nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "receipt_data", columnDefinition = "TEXT")
    private String receiptData;

    @Column(name = "printed_date")
    private LocalDateTime printedDate;

    @Column(name = "reprint_count")
    @Builder.Default
    private Integer reprintCount = 0;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "kra_pin", length = 20)
    private String kraPin;

    @Column(name = "qr_code_content", length = 2000)
    private String qrCodeContent;

    @Column(name = "verification_url", length = 1000)
    private String verificationUrl;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
