package com.example.pos.procurement.goodsreceived.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
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
@Table(name = "goods_received_notes")
public class GoodsReceivedNotes extends BaseEntity {
    //link purchase order id, link user id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_orders_id")
    private PurchaseOrders purchaseOrders;


    private LocalDateTime receivedAt;
    private String remarks;
}
