package com.example.pos.customer.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.pharmacy.model.Pharmacy;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customer")
public class Customer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;

    @Builder.Default
    private Integer loyaltyPoints = 0;

    private String notes;
}
