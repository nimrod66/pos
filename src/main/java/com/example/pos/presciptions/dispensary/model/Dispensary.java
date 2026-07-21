package com.example.pos.presciptions.dispensary.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.presciptions.prescriptionitems.model.PrescriptionItems;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "dispensed_items")
public class Dispensary extends BaseEntity {
    //link batch id, user/pharmacist id, prescription item id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_items_id")
    private PrescriptionItems prescriptionItems;

    private Integer dispensedQuantity;
    private LocalDateTime dispensingDate;

}
