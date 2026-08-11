package com.example.pos.sale.sales.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.customer.model.Customer;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.payment.model.Payment;
import com.example.pos.sale.receipts.model.Receipts;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.user.users.model.User;
import com.example.pos.user.staffshifts.model.StaffShifts;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Column(unique = true, nullable = false, length = 64)
    private String invoiceNumber;

    @Column(name = "client_sale_id", unique = true, nullable = false)
    private java.util.UUID clientSaleId;

    @Builder.Default
    @OneToMany(mappedBy = "sales", cascade = CascadeType.ALL)
    private List<SaleItems> saleItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "sales", cascade = CascadeType.ALL)
    private Set<Receipts> receipts = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "sales", cascade = CascadeType.ALL)
    private Set<Payment> payment = new HashSet<>();

    @OneToMany(mappedBy = "sales", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SaleReturns> saleReturns;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private StaffShifts shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idempotency_id")
    private IdempotencyKey idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescriptions prescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;
    @Column(nullable = false)
    private BigDecimal subtotal;
    @Column(nullable = false)
    private BigDecimal discountTotal;
    @Column(nullable = false)
    private BigDecimal tax;
    @Column(nullable = false)
    private BigDecimal total;
    @Column(nullable = false)
    private BigDecimal paidTotal;
    @Column(nullable = false)
    private BigDecimal cashTendered;
    @Column(nullable = false)
    private BigDecimal changeDue;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(length = 500)
    private String note;
    @Column(nullable = false)
    private LocalDateTime completedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus saleStatus;

    private String terminalId;

    @Builder.Default
    private boolean synced = false;

    public enum PaymentStatus {
        PAID, NOT_PAID, IN_PROGRESS
    }

    public enum SaleStatus {
        COMPLETED, DONE, CANCELLED, SUSPENDED
    }

}

//Remember also to add payments
