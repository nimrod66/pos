package com.example.pos.finance.expensecategory.model;

import com.example.pos.common.BaseEntity;
import com.example.pos.finance.expenses.model.Expenses;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "expense_category")
public class ExpenseCategory extends BaseEntity {
    @OneToMany(mappedBy = "expenseCategory", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Expenses> expenses;

    private String categoryName;
    private String categoryDescription;
}
