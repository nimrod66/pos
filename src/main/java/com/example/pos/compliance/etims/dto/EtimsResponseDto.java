package com.example.pos.compliance.etims.dto;

import com.example.pos.compliance.etims.model.Etims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtimsResponseDto {

    private Long id;
    private Long saleId;
    private String invoiceNumber;
    private String submissionStatus;
    private String qrCode;
    private LocalDateTime createdAt;

    public static EtimsResponseDto from(Etims e) {
        return EtimsResponseDto.builder()
                .id(e.getId())
                .saleId(e.getSales() != null ? e.getSales().getId() : null)
                .invoiceNumber(e.getSales() != null ? e.getSales().getInvoiceNumber() : null)
                .submissionStatus(e.getSubmissionStatus()).qrCode(e.getQrCode())
                .createdAt(e.getCreatedAt()).build();
    }
}
