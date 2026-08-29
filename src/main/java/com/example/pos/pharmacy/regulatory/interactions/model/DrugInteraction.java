package com.example.pos.pharmacy.regulatory.interactions.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drug_interactions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"medicine_1_id", "medicine_2_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class DrugInteraction extends BaseEntity {

    public enum Severity { MINOR, MODERATE, MAJOR, CONTRAINDICATED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_1_id")
    private Medicine medicine1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_2_id")
    private Medicine medicine2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
