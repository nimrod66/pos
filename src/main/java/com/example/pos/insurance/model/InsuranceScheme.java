package com.example.pos.insurance.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "insurance_schemes")
public class InsuranceScheme extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Column(name = "co_pay_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal coPayPercentage;

    @Column(name = "co_pay_flat", precision = 15, scale = 2)
    private java.math.BigDecimal coPayFlat;

    @Column(name = "max_claim_amount", precision = 15, scale = 2)
    private java.math.BigDecimal maxClaimAmount;

    @Column(name = "requires_preauth")
    private boolean requiresPreauth;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SchemeStatus status = SchemeStatus.ACTIVE;

    public enum SchemeStatus { ACTIVE, INACTIVE }
}
