package com.example.pos.finance.expenses.dto;

import com.example.pos.finance.expenses.model.Expenses;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpensesResponseDto {

    private UUID id;
    private UUID expenseCategoryId;
    private String categoryName;
    private UUID cashDrawersId;
    private UUID userId;
    private String userName;
    private BigDecimal amount;
    private String description;
    private LocalDateTime expenseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpensesResponseDto from(Expenses e) {
        return ExpensesResponseDto.builder()
                .id(e.getId())
                .expenseCategoryId(e.getExpenseCategory() != null ? e.getExpenseCategory().getId() : null)
                .categoryName(e.getExpenseCategory() != null ? e.getExpenseCategory().getCategoryName() : null)
                .cashDrawersId(e.getCashDrawers() != null ? e.getCashDrawers().getId() : null)
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userName(e.getUser() != null ? e.getUser().getFirstName() : null)
                .amount(e.getAmount())
                .description(e.getDescription())
                .expenseDate(e.getExpenseDate())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

