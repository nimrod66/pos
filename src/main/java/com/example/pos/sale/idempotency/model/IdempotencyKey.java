package com.example.pos.sale.idempotency.model;

import com.example.pos.common.BaseEntity;

import com.example.pos.sale.sales.model.Sales;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "idempotency")
public class IdempotencyKey extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "idempotencyKey", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Sales> sales = new HashSet<>();

    private String idempotencyKey;
    private String requestHash;
    private String resourceType;
    private String resourceId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalTime createdTime;
    private LocalTime expiresAt;

    public enum Status {
        IN_PROGRESS, COMPLETED, FAILED
    }
}
