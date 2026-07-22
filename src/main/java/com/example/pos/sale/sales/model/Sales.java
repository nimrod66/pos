package com.example.pos.sale.sales.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.customer.model.Customer;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.receipts.model.Receipts;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "sales")
public class Sales extends BaseEntity {
    @Column(length = 36, unique = true, nullable = false)
    @Builder.Default
    private String uuid = java.util.UUID.randomUUID().toString();

    private String invoiceNumber;

    @OneToMany(mappedBy = "sales", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SaleItems> saleItems;

    @Builder.Default
    @OneToMany(mappedBy = "sales", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Receipts> receipts = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "sales", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Payment> payment = new HashSet<>();

    @OneToMany(mappedBy = "sales", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SaleReturns> saleReturns;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idempotency_id")
    private IdempotencyKey idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    @Enumerated(EnumType.STRING)
    private SaleStatus saleStatus;

    private String terminalId;

    @Builder.Default
    private boolean synced = false;

    public enum PaymentStatus {
        PAID, NOT_PAID, IN_PROGRESS
    }

    public enum SaleStatus {
        DONE, CANCELLED, SUSPENDED
    }

}

//Remember also to add payments
