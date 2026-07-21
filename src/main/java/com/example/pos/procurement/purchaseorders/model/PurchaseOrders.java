package com.example.pos.procurement.purchaseorders.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "purchase_orders")
public class PurchaseOrders extends BaseEntity {
    //link supplier id, branch id, ordered by, approved by(user id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User orderedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suppliers_id")
    private Suppliers supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrders", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PurchaseOrderItems> purchaseOrderItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrders", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<GoodsReceivedNotes> goodsReceivedNotes = new HashSet<>();

    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime deliveryDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ORDERED, DELIVERED, IN_PROGRESS, FAILED //will change
    }
}
