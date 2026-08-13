package com.example.pos.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    private UUID pharmacyId;
    private UUID branchId;
    private String branchName;
    private LocalDate from;
    private LocalDate to;
    private long salesCount;
    private BigDecimal salesTotal;
    private long refundCount;
    private BigDecimal refundTotal;
    private BigDecimal netSales;
    private BigDecimal expensesTotal;
    private long lowStockCount;
    private long totalStockItems;
}
