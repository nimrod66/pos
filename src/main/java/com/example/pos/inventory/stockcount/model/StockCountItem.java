package com.example.pos.inventory.stockcount.model;

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
@Table(name = "stock_count_items")
public class StockCountItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_batches_id", nullable = false)
    private MedicineBatches medicineBatches;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "counted_quantity")
    private Integer countedQuantity;

    @Column(name = "variance", insertable = false, updatable = false)
    private Integer variance;

    private String remarks;
}
