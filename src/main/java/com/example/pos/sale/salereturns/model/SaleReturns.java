package com.example.pos.sale.salereturns.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import com.example.pos.sale.sales.model.Sales;
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
@Table(name = "sale_returns")
public class SaleReturns extends BaseEntity {
    //link sale id, processed by id

    @Builder.Default
    @OneToMany(mappedBy = "saleReturns", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SaleReturnItems> saleReturnItems = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id")
    private Sales sales;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String reason;
    private LocalDateTime returnDate;

    private String status;


}
