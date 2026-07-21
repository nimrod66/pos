package com.example.pos.presciptions.prescriptions.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.compliance.controlledrugs.model.ControlledDrugs;
import com.example.pos.presciptions.prescriptionitems.model.PrescriptionItems;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "prescriptions")
public class Prescriptions extends BaseEntity {

    @Builder.Default
    @OneToMany(mappedBy = "prescriptions", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PrescriptionItems> prescriptionItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "prescriptions", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<ControlledDrugs> controlledDrugs = new HashSet<>();

    private String customerName;
    private String doctorName;
    private String doctorLicenseNumber;
    private String hospitalName;
    private String prescriptionNumber;
    private String diagnosis;
    private LocalDate issuedDate;

    private String status;


}
