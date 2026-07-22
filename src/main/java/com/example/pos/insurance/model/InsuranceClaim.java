package com.example.pos.insurance.model;

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
@Table(name = "insurance_claims")
public class InsuranceClaim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id")
    private InsuranceScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private InsuranceMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id")
    private Authorization authorization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ClaimBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private InsurancePayment payment;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "patient_name", length = 100)
    private String patientName;

    @Column(name = "patient_membership_id", length = 50)
    private String patientMembershipId;

    @Column(name = "claim_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal claimAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Column(name = "rejected_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal rejectedAmount = BigDecimal.ZERO;

    @Column(name = "co_pay_amount", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal coPayAmount = BigDecimal.ZERO;

    @Column(name = "sale_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal saleTotal;

    @Column(name = "claim_reference", length = 50, unique = true)
    private String claimReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false, length = 25)
    @Builder.Default
    private ClaimStatus claimStatus = ClaimStatus.PENDING;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(length = 500)
    private String notes;
}
