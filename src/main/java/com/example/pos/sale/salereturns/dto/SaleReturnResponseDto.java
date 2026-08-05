package com.example.pos.sale.salereturns.dto;

import com.example.pos.sale.salereturns.model.SaleReturns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturnResponseDto {

    private UUID id;
    private UUID saleId;
    private String invoiceNumber;
    private UUID userId;
    private String userName;
    private String reason;
    private String status;
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
    }

    public static SaleReturnResponseDto from(SaleReturns sr) {
        return SaleReturnResponseDto.builder()
                .id(sr.getId())
                .saleId(sr.getSales() != null ? sr.getSales().getId() : null)
                .invoiceNumber(sr.getSales() != null ? sr.getSales().getInvoiceNumber() : null)
                .userId(sr.getUser() != null ? sr.getUser().getId() : null)
                .userName(sr.getUser() != null ? sr.getUser().getFirstName() : null)
                .reason(sr.getReason())
                .status(sr.getStatus())
                .returnDate(sr.getReturnDate())
                .createdAt(sr.getCreatedAt())
                .build();
    }
}

