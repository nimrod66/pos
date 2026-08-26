package com.example.pos.finance.expenses.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDate;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpensesRequestDto {

    @NotNull(message = "Expense category ID is required")
    private UUID expenseCategoryId;

    private UUID cashDrawersId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull @Positive
    private BigDecimal amount;

    private String description;

    private LocalDate expenseDate;
}

