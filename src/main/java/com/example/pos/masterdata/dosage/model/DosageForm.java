package com.example.pos.masterdata.dosage.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "dosage_form")
@Builder
public class DosageForm extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "dosageForm", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Medicine> medicine = new HashSet<>();

    private String formName;
    private String formDescription;


}
