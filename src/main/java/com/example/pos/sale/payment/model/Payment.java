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
    @JoinColumn(name = "sales_id")
    private Sales sales;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private BigDecimal amount;
    private String currency;
    private String description;
    private String transactionReference;
    private String paymentStatus;
    private LocalDateTime paymentDate;

    public enum PaymentMethod {
        M_PESA, CASH, CARD
    }

}
