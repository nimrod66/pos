package com.example.pos.inventory.stock.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.inventory.batches.model.MedicineBatches;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "stock")
public class Stock extends BaseEntity {
    //link batch id, branch id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private Integer quantityAvailable;
    private Integer reservedQuantity;
    private Integer minimumStock;
    private Integer maximumStock;
    private Integer reorderLevel;
    private String shelfLocation;
    private LocalDate lastStockDate;
}
