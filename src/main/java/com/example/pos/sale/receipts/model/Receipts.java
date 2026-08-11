package com.example.pos.sale.receipts.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.sale.sales.model.Sales;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "receipts")
public class Receipts extends BaseEntity {
    //link sale id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", nullable = false)
    private Sales sales;

    @Column(nullable = false, unique = true, length = 64)
    private String receiptNumber;
    private LocalDateTime printedDate;
}
