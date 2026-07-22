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
@Table(name = "insurers")
public class Insurer extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurer_type", nullable = false, length = 20)
    private InsurerType insurerType;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(name = "claim_submission_email", length = 100)
    private String claimSubmissionEmail;

    @Column(name = "preauth_phone", length = 20)
    private String preauthPhone;

    @Column(name = "default_co_pay_percentage", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal defaultCoPayPercentage = java.math.BigDecimal.ZERO;

    @Column(name = "default_co_pay_flat", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal defaultCoPayFlat = java.math.BigDecimal.ZERO;

    @Column(name = "requires_preauth")
    @Builder.Default
    private boolean requiresPreauth = false;

    @Column(name = "max_claim_amount", precision = 15, scale = 2)
    private java.math.BigDecimal maxClaimAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public enum InsurerType {
        GOVERNMENT,
        PRIVATE,
        CORPORATE,
        SELF_PAY
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
}
