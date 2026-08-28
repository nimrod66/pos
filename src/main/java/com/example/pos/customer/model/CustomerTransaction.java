package com.example.pos.customer.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customer_transactions")
public class CustomerTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sales sale;

    @Column(nullable = false, length = 30)
    private String transactionType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal runningBalance;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    public enum TransactionType {
        SALE, PAYMENT, ADJUSTMENT, CREDIT_ISSUED
    }
}
