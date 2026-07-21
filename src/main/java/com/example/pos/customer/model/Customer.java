package com.example.pos.customer.model;

import com.example.pos.common.BaseEntity;
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

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;

    @Builder.Default
    private Integer loyaltyPoints = 0;

    private String notes;
}
