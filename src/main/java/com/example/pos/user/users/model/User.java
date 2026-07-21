package com.example.pos.user.users.model;

import com.example.pos.audit.model.AuditLog;
import com.example.pos.compliance.controlledrugs.model.ControlledDrugs;
import com.example.pos.compliance.expiry.model.ExpiryLogs;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.presciptions.dispensary.model.Dispensary;
import com.example.pos.procurement.goodsreceived.model.GoodsReceivedNotes;
import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.user.loginhistory.model.LoginHistory;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import jakarta.persistence.*;

import com.example.pos.common.BaseEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user")
@Builder

public class User extends BaseEntity {

    private String firstName;
    private String middleName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime lastLogin;

    public enum Status {
        ACTIVE, INACTIVE, TRANSFERRED
    }

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<LoginHistory> loginHistories;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<StaffShifts> staffShifts;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<StockMovements> stockMovements = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SupplierPayment> supplierPayment;

    @OneToMany(mappedBy = "orderedBy", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PurchaseOrders> purchaseOrders;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<GoodsReceivedNotes> goodsReceivedNotes = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<PriceHistory> priceHistory = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Sales> sales = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<SaleReturns> saleReturns = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Expenses> expenses = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Dispensary> dispensary = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<ControlledDrugs> controlledDrugs = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<ExpiryLogs> expiryLogs = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<UserBranchRole> userBranchRole = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<AuditLog> auditLog;


}
