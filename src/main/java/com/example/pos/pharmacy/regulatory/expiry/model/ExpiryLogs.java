package com.example.pos.pharmacy.regulatory.expiry.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "expiry")
public class ExpiryLogs extends BaseEntity {
    //link batch id, recorded by
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String disposalMethod;
}
