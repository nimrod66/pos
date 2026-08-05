package com.example.pos.finance.cashtransactions.dto;

import com.example.pos.finance.cashtransactions.model.CashTransactions;
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
public class CashTransactionResponseDto {
    private UUID id;
    private UUID cashDrawerId;
    private String transactionType;
    private BigDecimal amount;
    private String remarks;
    private String referenceType;
    private String referenceId;
    private LocalDateTime createdAt;

    public static CashTransactionResponseDto from(CashTransactions ct) {
        return CashTransactionResponseDto.builder()
                .id(ct.getId())
                .cashDrawerId(ct.getCashDrawers() != null ? ct.getCashDrawers().getId() : null)
                .transactionType(ct.getTransactionType())
                .amount(ct.getAmount())
                .remarks(ct.getRemarks())
                .referenceType(ct.getReferenceType())
                .referenceId(ct.getReferenceId())
                .createdAt(ct.getCreatedAt())
                .build();
    }
}

