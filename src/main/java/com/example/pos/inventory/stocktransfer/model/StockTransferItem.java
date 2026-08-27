package com.example.pos.inventory.stocktransfer.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "stock_transfer_items")
public class StockTransferItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_batches_id", nullable = false)
    private MedicineBatches medicineBatches;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "received_quantity")
    @Builder.Default
    private Integer receivedQuantity = 0;
}
