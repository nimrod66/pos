package com.example.pos.finance.expensecategory.dto;

import com.example.pos.finance.expensecategory.model.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategoryResponseDto {

    private UUID id;
    private String categoryName;
    private String categoryDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpenseCategoryResponseDto from(ExpenseCategory ec) {
        return ExpenseCategoryResponseDto.builder()
                .id(ec.getId())
                .categoryName(ec.getCategoryName())
                .categoryDescription(ec.getCategoryDescription())
                .createdAt(ec.getCreatedAt())
                .updatedAt(ec.getUpdatedAt())
                .build();
    }
}

