package com.example.pos.insurance.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "insurance_members")
public class InsuranceMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @Column(name = "membership_number", nullable = false, length = 50)
    private String membershipNumber;

    @Column(name = "member_name", length = 100)
    private String memberName;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(length = 20)
    private String phone;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    public enum MemberStatus { ACTIVE, EXPIRED, INACTIVE }
}
