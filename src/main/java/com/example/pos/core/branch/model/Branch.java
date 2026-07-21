package com.example.pos.core.branch.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "branch")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Branch extends BaseEntity {


    private String branchName;
    private String branchCode;
    private String phoneNumber;
    private String email;
    private String location;
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ACTIVE, INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Builder.Default
    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SystemSettings> systemSettings = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<StaffShifts> staffShifts = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Stock> stock = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<StockMovements> stockMovements = new HashSet<>();

    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PurchaseOrders> purchaseOrders;

    @Builder.Default
    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Sales> sales = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "branch", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<UserBranchRole> userBranchRole = new HashSet<>();


}
