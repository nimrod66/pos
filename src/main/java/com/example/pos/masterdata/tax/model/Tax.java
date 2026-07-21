package com.example.pos.masterdata.tax.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "tax_category")
public class Tax extends BaseEntity {
    @Builder.Default
    @OneToMany(mappedBy = "tax", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Medicine> medicine = new HashSet<>();

    @Column(unique = true, nullable = false)
    private String code;

    private String taxName;

    @Column(name = "tax_description")
    private String taxDescription;

    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type")
    private TaxType taxType;

    @Builder.Default
    private boolean active = true;
}
