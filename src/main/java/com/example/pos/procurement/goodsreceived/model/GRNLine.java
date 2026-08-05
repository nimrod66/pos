package com.example.pos.procurement.goodsreceived.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.masterdata.medicine.model.Medicine;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "grn_lines")
public class GRNLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceivedNotes goodsReceivedNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private MedicineBatches batch;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "purchase_order_line_id")
    private java.util.UUID purchaseOrderLineId;
}
