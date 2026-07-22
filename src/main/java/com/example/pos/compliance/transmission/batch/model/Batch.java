package com.example.pos.compliance.transmission.batch.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "compliance_batches")
public class Batch extends BaseEntity {

    @Column(name = "batch_reference", unique = true, nullable = false, length = 50)
    private String batchReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_status", nullable = false, length = 20)
    @Builder.Default
    private BatchStatus batchStatus = BatchStatus.BUILDING;

    @Column(name = "provider", nullable = false, length = 10)
    @Builder.Default
    private String provider = "VSCU";

    @Column(name = "invoice_count")
    @Builder.Default
    private int invoiceCount = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "batch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BatchItem> items = new ArrayList<>();
}
