package com.example.pos.insurance.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "claim_reconciliations")
public class ClaimReconciliation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_claimed", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalClaimed = BigDecimal.ZERO;

    @Column(name = "total_approved", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalApproved = BigDecimal.ZERO;

    @Column(name = "total_rejected", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRejected = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "outstanding", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal outstanding = BigDecimal.ZERO;

    @Column(name = "claim_count")
    private int claimCount;

    @Column(name = "settled_count")
    private int settledCount;

    @Column(length = 500)
    private String notes;
}
