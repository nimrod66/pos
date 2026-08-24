package com.example.pos.sale.payment.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.sale.sales.model.Sales;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "payments")
public class Payment extends BaseEntity {
    //link sale id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", nullable = false)
    private Sales sales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    private String description;
    private String transactionReference;
    @Column(length = 100)
    private String merchantRequestId;
    @Column(length = 100)
    private String checkoutRequestId;
    @Column(nullable = false)
    private String paymentStatus;
    private LocalDateTime paymentDate;

    public enum PaymentMethod {
        MPESA_MANUAL, M_PESA, CASH, CARD, STRIPE
    }

}
