package com.example.pos.finance.expensecategory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryRequestDto {

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String categoryDescription;
}
