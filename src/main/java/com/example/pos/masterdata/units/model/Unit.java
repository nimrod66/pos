package com.example.pos.masterdata.units.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "unit_of_measure")
public class Unit extends BaseEntity {
    @Builder.Default
    @OneToMany(mappedBy = "unit", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Medicine> medicine = new HashSet<>();

    private String unitName;
    private String unitAbbreviation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private Unit parentUnit;

    @Column(name = "conversion_factor")
    @Builder.Default
    private Integer conversionFactor = 1;
}
