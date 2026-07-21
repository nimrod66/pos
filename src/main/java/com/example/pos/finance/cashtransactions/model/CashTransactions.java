package com.example.pos.finance.cashtransactions.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "cash_transactions")
public class CashTransactions extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_drawers_id")
    private CashDrawers cashDrawers;

    private String transactionType;
    private BigDecimal amount;
    private String remarks;
    private String referenceType;
    private String referenceId;

}
