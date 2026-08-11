package com.example.pos.prescriptions.prescriptions.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.pharmacy.regulatory.controlledrugs.model.ControlledDrugs;
import com.example.pos.prescriptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.user.users.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

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
    private LocalDateTime approvedAt;
    private LocalDateTime dispensedAt;


}
