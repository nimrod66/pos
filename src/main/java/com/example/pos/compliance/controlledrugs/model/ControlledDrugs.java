package com.example.pos.compliance.controlledrugs.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.presciptions.prescriptions.model.Prescriptions;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "controlled_drugs")
public class ControlledDrugs extends BaseEntity {
    //medicine id, customer name, pharmacist id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescriptions_id")
    private Prescriptions prescriptions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer quantityDispensed;
}
