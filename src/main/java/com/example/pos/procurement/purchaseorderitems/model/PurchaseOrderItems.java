package com.example.pos.procurement.purchaseorderitems.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.masterdata.medicine.model.Medicine;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_order_items")
public class PurchaseOrderItems extends BaseEntity {
    //link medicine id, purchase order id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_orders_id")
    private PurchaseOrders purchaseOrders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private Integer quantity;
    private BigDecimal buyingPrice;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
}
