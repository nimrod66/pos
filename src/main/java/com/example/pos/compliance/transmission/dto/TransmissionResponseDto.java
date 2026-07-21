package com.example.pos.compliance.transmission.dto;

import com.example.pos.compliance.transmission.model.Transmission;
import com.example.pos.compliance.transmission.model.TransmissionAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransmissionResponseDto {

    private Long id;
    private Long invoiceId;
    private String documentType;
    private String transmissionStatus;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private String requestHash;
    private String responseHash;
    private Integer payloadVersion;
    private String kraReceiptNumber;
    private String failureReason;
    private LocalDateTime nextRetryTime;
    private List<AttemptResponse> attempts;
    private LocalDateTime createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttemptResponse {
        private Long id;
        private Integer attemptNumber;
        private LocalDateTime sentAt;
        private LocalDateTime responseAt;
        private boolean success;
        private Integer statusCode;
        private String errorMessage;
        private Long durationMs;
    }

    public static TransmissionResponseDto from(Transmission tx) {
        var builder = TransmissionResponseDto.builder()
                .id(tx.getId())
                .invoiceId(tx.getInvoiceId())
                .documentType(tx.getDocumentType())
                .transmissionStatus(tx.getTransmissionStatus() != null ? tx.getTransmissionStatus().name() : null)
                .submittedBy(tx.getSubmittedBy())
                .submittedAt(tx.getSubmittedAt())
                .requestHash(tx.getRequestHash())
                .responseHash(tx.getResponseHash())
                .payloadVersion(tx.getPayloadVersion())
                .kraReceiptNumber(tx.getKraReceiptNumber())
                .failureReason(tx.getFailureReason())
                .nextRetryTime(tx.getNextRetryTime())
                .createdAt(tx.getCreatedAt());

        if (tx.getAttempts() != null) {
            builder.attempts(tx.getAttempts().stream().map(a -> AttemptResponse.builder()
                    .id(a.getId())
                    .attemptNumber(a.getAttemptNumber())
                    .sentAt(a.getSentAt())
                    .responseAt(a.getResponseAt())
                    .success(a.isSuccess())
                    .statusCode(a.getStatusCode())
                    .errorMessage(a.getErrorMessage())
                    .durationMs(a.getDurationMs())
                    .build()).toList());
        }

        return builder.build();
    }
}
