package com.example.pos.compliance.invoice.dto;

import com.example.pos.compliance.invoice.model.DebitNote;
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
public class DebitNoteResponseDto {

    private UUID id;
    private UUID originalInvoiceId;
    private String debitNoteNumber;
    private String reason;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String status;
    private LocalDateTime issueDate;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DebitNoteResponseDto from(DebitNote dn) {
        return DebitNoteResponseDto.builder()
                .id(dn.getId())
                .originalInvoiceId(dn.getOriginalInvoiceId())
                .debitNoteNumber(dn.getDebitNoteNumber())
                .reason(dn.getReason())
                .amount(dn.getAmount())
                .taxAmount(dn.getTaxAmount())
                .status(dn.getStatus() != null ? dn.getStatus().name() : null)
                .issueDate(dn.getIssueDate())
                .createdBy(dn.getCreatedBy())
                .createdAt(dn.getCreatedAt())
                .updatedAt(dn.getUpdatedAt())
                .build();
    }
}