package com.example.pos.sale.salereturns.dto;

import com.example.pos.sale.salereturns.model.SaleReturns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturnResponseDto {

    private UUID id;
    private UUID clientReturnId;
    private UUID saleId;
    private String invoiceNumber;
    private UUID userId;
    private String userName;
    private String reason;
    private String status;
    private UUID branchId;
    private UUID shiftId;
    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundReference;
    private LocalDateTime returnDate;
    private List<ReturnItemResponse> items;
    private LocalDateTime createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReturnItemResponse {
        private UUID id;
        private UUID saleItemId;
        private UUID medicineBatchesId;
        private String batchNumber;
        private String medicineName;
        private Integer quantity;
        private BigDecimal refundAmount;
        private String disposition;
    }

    public static SaleReturnResponseDto from(SaleReturns sr) {
        return SaleReturnResponseDto.builder()
                .id(sr.getId())
                .clientReturnId(sr.getClientReturnId())
                .saleId(sr.getSales() != null ? sr.getSales().getId() : null)
                .invoiceNumber(sr.getSales() != null ? sr.getSales().getInvoiceNumber() : null)
                .userId(sr.getUser() != null ? sr.getUser().getId() : null)
                .userName(sr.getUser() != null ? sr.getUser().getFirstName() : null)
                .reason(sr.getReason())
                .status(sr.getStatus())
                .branchId(sr.getBranch() != null ? sr.getBranch().getId() : null)
                .shiftId(sr.getStaffShift() != null ? sr.getStaffShift().getId() : null)
                .refundAmount(sr.getRefundAmount())
                .refundMethod(sr.getRefundMethod())
                .refundReference(sr.getRefundReference())
                .returnDate(sr.getReturnDate())
                .createdAt(sr.getCreatedAt())
                .build();
    }
}

