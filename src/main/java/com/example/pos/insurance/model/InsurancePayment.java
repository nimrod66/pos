package com.example.pos.insurance.model;

import com.example.pos.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "insurance_payments")
public class InsurancePayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurer_id", nullable = false)
    private Insurer insurer;

    @Column(name = "payment_reference", nullable = false, length = 50, unique = true)
    private String paymentReference;

    @Column(name = "bank_reference", length = 50)
    private String bankReference;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "receipt_path", length = 500)
    private String receiptPath;

    @Column(length = 500)
    private String notes;

    public enum PaymentMethod { BANK_TRANSFER, CHEQUE, M_PESA, CASH }
}
