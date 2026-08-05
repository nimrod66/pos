package com.example.pos.masterdata.categories.dto;

import com.example.pos.masterdata.categories.model.MedicineCategories;
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
public class CategoryResponseDto {

    private UUID id;
    private String categoryName;
    private String categoryDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponseDto from(MedicineCategories category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .categoryDescription(category.getCategoryDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}

