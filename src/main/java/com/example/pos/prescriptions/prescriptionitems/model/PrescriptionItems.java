package com.example.pos.prescriptions.prescriptionitems.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.prescriptions.dispensary.model.Dispensary;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "prescription_items")
public class PrescriptionItems extends BaseEntity {
    // link medicine id, prescription id

    @Builder.Default
    @OneToMany(mappedBy = "prescriptionItems", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Dispensary> dispensary = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescriptions prescriptions;

    private String dosage;
    private Integer quantity;
}
