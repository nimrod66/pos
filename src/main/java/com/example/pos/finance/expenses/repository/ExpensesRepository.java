package com.example.pos.finance.expenses.repository;

import java.util.UUID;

import com.example.pos.finance.expenses.model.Expenses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpensesRepository extends JpaRepository<Expenses, UUID> {

    List<Expenses> findByUserId(UUID userId);

    List<Expenses> findByExpenseCategoryId(UUID categoryId);

    List<Expenses> findByCashDrawersId(UUID cashDrawerId);

    List<Expenses> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);
}
