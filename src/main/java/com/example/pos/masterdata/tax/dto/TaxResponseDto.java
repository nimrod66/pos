package com.example.pos.masterdata.tax.dto;

import com.example.pos.masterdata.tax.model.Tax;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxResponseDto {

    private Long id;
    private String taxName;
    private String taxDescription;
    private BigDecimal taxRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaxResponseDto from(Tax tax) {
        return TaxResponseDto.builder()
                .id(tax.getId())
                .taxName(tax.getTaxName())
                .taxDescription(tax.getTaxDescription())
                .taxRate(tax.getTaxRate())
                .createdAt(tax.getCreatedAt())
                .updatedAt(tax.getUpdatedAt())
                .build();
    }
}
