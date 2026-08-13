package com.example.pos.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportResponseDto {
    private UUID pharmacyId;
    private UUID branchId;
    private String branchName;
    private long totalStockItems;
    private long lowStockItems;
    private long nearExpiryItems;
}
