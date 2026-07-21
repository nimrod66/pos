package com.example.pos.finance.expenses.repository;

import com.example.pos.finance.expenses.model.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpensesRepository extends JpaRepository<Expenses, Long> {

    List<Expenses> findByUserId(Long userId);

    List<Expenses> findByExpenseCategoryId(Long categoryId);

    List<Expenses> findByCashDrawersId(Long cashDrawerId);

    List<Expenses> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
