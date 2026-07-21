package com.example.pos.procurement.supplierinvoices.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.procurement.purchaseorderitems.model.PurchaseOrderItems;
import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import com.example.pos.procurement.suppliers.model.Suppliers;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "supplier_invoices")
@Builder
public class SupplierInvoices extends BaseEntity {
    //link supplier id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suppliers_id")
    private Suppliers suppliers;

    @Builder.Default
    @OneToMany(mappedBy = "supplierInvoices", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SupplierPayment> supplierPayment = new HashSet<>();

    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal total;
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PAID, NOT_PAID
    }
}
