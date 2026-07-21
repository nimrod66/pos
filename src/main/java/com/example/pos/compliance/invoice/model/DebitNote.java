package com.example.pos.compliance.invoice.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "debit_notes")
public class DebitNote extends BaseEntity {

    @Column(name = "original_invoice_id", nullable = false)
    private Long originalInvoiceId;

    @Column(name = "debit_note_number", unique = true, nullable = false)
    private String debitNoteNumber;

    @Column(length = 1000)
    private String reason;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CreditNoteStatus status = CreditNoteStatus.DRAFT;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "tenant_id")
    private Long tenantId;
}
