package com.example.pos.compliance.batch.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "compliance_batch_items")
public class BatchItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "transmission_status", length = 20)
    private String transmissionStatus;

    @Column(name = "kra_receipt_number", length = 100)
    private String kraReceiptNumber;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
