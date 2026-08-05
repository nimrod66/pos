package com.example.pos.compliance.invoice.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "invoice_history")
public class InvoiceHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private TaxInvoice invoice;

    @Column(name = "history_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private InvoiceHistoryType historyType;

    @Column(length = 2000)
    private String description;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "metadata", length = 4000)
    private String metadata;
}
