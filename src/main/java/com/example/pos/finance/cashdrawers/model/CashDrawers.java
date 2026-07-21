package com.example.pos.finance.cashdrawers.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.finance.cashtransactions.model.CashTransactions;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.staffshifts.model.StaffShifts;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "cash_drawers")
public class CashDrawers extends BaseEntity {
    //link shift id
    @Builder.Default
    @OneToMany(mappedBy = "cashDrawers", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<CashTransactions> cashTransactions = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "cashDrawers", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Expenses> expenses = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_shifts_id")
    private StaffShifts staffShifts;

    private BigDecimal openingBalance;
    private BigDecimal expectedClosingBalance;
    private BigDecimal actualClosingBalance;
    private BigDecimal variance;
    private LocalTime openingTime;
    private LocalTime closingTime;

    private String status;


}
