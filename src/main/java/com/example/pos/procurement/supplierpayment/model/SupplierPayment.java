package com.example.pos.procurement.supplierpayment.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "supplier_payment")
public class SupplierPayment extends BaseEntity {
    //link supplier invoice id, link user id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_invoices_id")
    private SupplierInvoices supplierInvoices;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String paymentMethod;
    private BigDecimal paymentAmount;
    private String paymentReference;
    private LocalDateTime paymentDate;

}
