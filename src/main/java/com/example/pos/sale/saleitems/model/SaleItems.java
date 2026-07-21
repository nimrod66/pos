package com.example.pos.sale.saleitems.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import com.example.pos.sale.sales.model.Sales;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "sales_items")
public class SaleItems extends BaseEntity {
    // link sale id, batch id

    @Builder.Default
    @OneToMany(mappedBy = "saleItems", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SaleReturnItems> saleReturnItems = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_id")
    private Sales sales;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_batches_id")
    private MedicineBatches medicineBatches;

    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    @Column(name = "tax_rate")
    private BigDecimal taxRate;
    @Column(name = "taxable_amount")
    private BigDecimal taxableAmount;
    private BigDecimal tax;
    private BigDecimal total;
}


//Remember to add sale return items