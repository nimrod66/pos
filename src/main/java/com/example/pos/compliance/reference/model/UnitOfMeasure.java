package com.example.pos.compliance.reference.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "kra_unit_of_measure")
public class UnitOfMeasure extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}