package com.example.pos.sale.salereturns.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "sale_returns")
public class SaleReturns extends BaseEntity {
    //link sale id, processed by id

    @Builder.Default
    @OneToMany(mappedBy = "saleReturns", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SaleReturnItems> saleReturnItems = new HashSet<>();

    @Column(name = "client_return_id", nullable = false, unique = true)
    private UUID clientReturnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id", nullable = false)
    private Sales sales;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_shift_id", nullable = false)
    private StaffShifts staffShift;

    @Column(nullable = false, length = 500)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime returnDate;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private String refundMethod;

    private String refundReference;


}
