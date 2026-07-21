package com.example.pos.finance.expensecategory.repository;

import com.example.pos.finance.expensecategory.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByCategoryName(String name);

    boolean existsByCategoryName(String name);

    boolean existsByCategoryNameAndIdNot(String name, Long id);
}
