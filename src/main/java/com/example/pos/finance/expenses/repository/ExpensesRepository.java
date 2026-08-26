package com.example.pos.finance.expenses.repository;

import java.util.UUID;

import com.example.pos.finance.expenses.model.Expenses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExpensesRepository extends JpaRepository<Expenses, UUID> {

    @EntityGraph(attributePaths = {"expenseCategory", "user", "cashDrawers"})
    Page<Expenses> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"expenseCategory", "user", "cashDrawers"})
    List<Expenses> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"expenseCategory", "user", "cashDrawers"})
    List<Expenses> findByExpenseCategoryId(UUID categoryId);

    @EntityGraph(attributePaths = {"expenseCategory", "user", "cashDrawers"})
    List<Expenses> findByCashDrawersId(UUID cashDrawerId);

    @EntityGraph(attributePaths = {"expenseCategory", "user", "cashDrawers"})
    List<Expenses> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    @Override
    @EntityGraph(attributePaths = {"expenseCategory", "user", "cashDrawers"})
    Optional<Expenses> findById(UUID id);
}
