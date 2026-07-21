package com.example.pos.masterdata.manufacturer.model;

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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "manufacturer")
@Getter
@Setter
public class Manufacturer extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "manufacturer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Medicine> medicine = new HashSet<>();

    private String manufacturerName;
    private String manufacturerCountry;
    private String manufacturerContact;

}
