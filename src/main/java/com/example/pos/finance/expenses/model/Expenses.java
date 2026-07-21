package com.example.pos.finance.expenses.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.expensecategory.model.ExpenseCategory;
import com.example.pos.user.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "expenses")
public class Expenses extends BaseEntity {
    //link cash drawer id, expense category id, recorded by
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_category_id")
    private ExpenseCategory expenseCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_drawers_id")
    private CashDrawers cashDrawers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal amount;
    private String description;
    private LocalDateTime expenseDate;
}
