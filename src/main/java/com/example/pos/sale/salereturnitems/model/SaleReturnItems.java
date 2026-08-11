package com.example.pos.sale.salereturnitems.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.salereturns.model.SaleReturns;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "sale_return_items")
public class SaleReturnItems extends BaseEntity {

    // sales returns id, batch id, saleitem id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_items_id", nullable = false)
    private SaleItems saleItems;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id", nullable = false)
    private MedicineBatches medicineBatches;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_returns_id", nullable = false)
    private SaleReturns saleReturns;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private String disposition;


}
