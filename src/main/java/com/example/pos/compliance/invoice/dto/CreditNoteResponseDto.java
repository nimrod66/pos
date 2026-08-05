package com.example.pos.compliance.invoice.dto;

import com.example.pos.compliance.invoice.model.CreditNote;
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
public class CreditNoteResponseDto {

    private UUID id;
    private UUID originalInvoiceId;
    private String creditNoteNumber;
    private String reason;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String status;
    private LocalDateTime issueDate;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CreditNoteResponseDto from(CreditNote cn) {
        return CreditNoteResponseDto.builder()
                .id(cn.getId())
                .originalInvoiceId(cn.getOriginalInvoiceId())
                .creditNoteNumber(cn.getCreditNoteNumber())
                .reason(cn.getReason())
                .amount(cn.getAmount())
                .taxAmount(cn.getTaxAmount())
                .status(cn.getStatus() != null ? cn.getStatus().name() : null)
                .issueDate(cn.getIssueDate())
                .createdBy(cn.getCreatedBy())
                .createdAt(cn.getCreatedAt())
                .updatedAt(cn.getUpdatedAt())
                .build();
    }
}