package com.example.pos.finance.cashdrawers.dto;

import com.example.pos.finance.cashdrawers.model.CashDrawers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashDrawerResponseDto {

    private Long id;
    private Long staffShiftsId;
    private String status;
    private BigDecimal openingBalance;
    private BigDecimal expectedClosingBalance;
    private BigDecimal actualClosingBalance;
    private BigDecimal variance;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CashDrawerResponseDto from(CashDrawers cd) {
        return CashDrawerResponseDto.builder()
                .id(cd.getId())
                .staffShiftsId(cd.getStaffShifts() != null ? cd.getStaffShifts().getId() : null)
                .status(cd.getStatus())
                .openingBalance(cd.getOpeningBalance())
                .expectedClosingBalance(cd.getExpectedClosingBalance())
                .actualClosingBalance(cd.getActualClosingBalance())
                .variance(cd.getVariance())
                .openingTime(cd.getOpeningTime())
                .closingTime(cd.getClosingTime())
                .createdAt(cd.getCreatedAt())
                .updatedAt(cd.getUpdatedAt())
                .build();
    }
}
