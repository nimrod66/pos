package com.example.pos.inventory.stockmovements.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "stock_movements")
public class StockMovements extends BaseEntity {
    //link batch id, link branch id, linked perfomed by (user)
    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    public enum MovementType {
        PURCHASE, SALE, RETURN, TRANSFER, ADJUSTMENT, EXPIRED, DAMAGED, LOSS, DISPENSE, RESERVATION, RESERVATION_RELEASE
    }

    private String referenceType;
    private UUID referenceId;
    private LocalDate movementDate;
    private Integer quantity;

}
