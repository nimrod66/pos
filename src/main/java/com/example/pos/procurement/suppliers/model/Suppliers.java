package com.example.pos.procurement.suppliers.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "suppliers")
public class Suppliers extends BaseEntity {


    private String supplierName;
    private String licenseNumber;
    private String phoneNumber;
    private String address;
    private String email;
    private String contactPerson;
    private String paymentTerms;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ACTIVE, INACTIVE //will change
    }

    @Builder.Default
    @OneToMany(mappedBy = "supplier", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PurchaseOrders> purchaseOrders = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "suppliers", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SupplierInvoices> supplierInvoices = new HashSet<>();

}
