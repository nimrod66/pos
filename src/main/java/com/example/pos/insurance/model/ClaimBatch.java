package com.example.pos.insurance.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "claim_batches")
public class ClaimBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @Column(name = "batch_reference", nullable = false, length = 50, unique = true)
    private String batchReference;

    @Column(name = "claim_count")
    private int claimCount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    @Builder.Default
    private BatchStatus status = BatchStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(length = 500)
    private String notes;

    public enum BatchStatus { DRAFT, SUBMITTED, ACKNOWLEDGED, PROCESSING, SETTLED, REJECTED }
}
