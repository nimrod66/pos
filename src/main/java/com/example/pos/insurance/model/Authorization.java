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
@Table(name = "insurance_authorizations")
public class Authorization extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @Column(name = "authorization_reference", nullable = false, length = 50, unique = true)
    private String authorizationReference;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "used_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "authorized_by", length = 100)
    private String authorizedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    @Builder.Default
    private AuthStatus status = AuthStatus.ACTIVE;

    @Column(length = 500)
    private String notes;

    public enum AuthStatus { ACTIVE, EXHAUSTED, EXPIRED, REVOKED }

    public boolean hasRemainingBalance() {
        if (approvedAmount == null) return true;
        return usedAmount.compareTo(approvedAmount) < 0;
    }

    public BigDecimal remainingAmount() {
        if (approvedAmount == null) return null;
        BigDecimal remaining = approvedAmount.subtract(usedAmount);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }
}
