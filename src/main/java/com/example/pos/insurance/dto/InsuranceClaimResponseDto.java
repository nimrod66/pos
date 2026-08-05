package com.example.pos.insurance.dto;

import com.example.pos.insurance.model.InsuranceClaim;
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
public class InsuranceClaimResponseDto {
    private UUID id;
    private UUID insurerId;
    private String insurerName;
    private UUID schemeId;
    private String schemeName;
    private UUID memberId;
    private String memberName;
    private UUID authorizationId;
    private String authorizationRef;
    private UUID batchId;
    private String batchRef;
    private UUID paymentId;
    private String paymentRef;
    private UUID saleId;
    private String patientName;
    private String patientMembershipId;
    private BigDecimal claimAmount;
    private BigDecimal approvedAmount;
    private BigDecimal rejectedAmount;
    private BigDecimal coPayAmount;
    private BigDecimal saleTotal;
    private String claimReference;
    private String claimStatus;
    private LocalDateTime submittedAt;
    private String rejectionReason;
    private String notes;

    public static InsuranceClaimResponseDto from(InsuranceClaim c) {
        return InsuranceClaimResponseDto.builder()
                .id(c.getId())
                .insurerId(c.getInsurer() != null ? c.getInsurer().getId() : null)
                .insurerName(c.getInsurer() != null ? c.getInsurer().getName() : null)
                .schemeId(c.getScheme() != null ? c.getScheme().getId() : null)
                .schemeName(c.getScheme() != null ? c.getScheme().getName() : null)
                .memberId(c.getMember() != null ? c.getMember().getId() : null)
                .memberName(c.getMember() != null ? c.getMember().getMemberName() : null)
                .authorizationId(c.getAuthorization() != null ? c.getAuthorization().getId() : null)
                .authorizationRef(c.getAuthorization() != null ? c.getAuthorization().getAuthorizationReference() : null)
                .batchId(c.getBatch() != null ? c.getBatch().getId() : null)
                .batchRef(c.getBatch() != null ? c.getBatch().getBatchReference() : null)
                .paymentId(c.getPayment() != null ? c.getPayment().getId() : null)
                .paymentRef(c.getPayment() != null ? c.getPayment().getPaymentReference() : null)
                .saleId(c.getSaleId())
                .patientName(c.getPatientName())
                .patientMembershipId(c.getPatientMembershipId())
                .claimAmount(c.getClaimAmount())
                .approvedAmount(c.getApprovedAmount())
                .rejectedAmount(c.getRejectedAmount())
                .coPayAmount(c.getCoPayAmount())
                .saleTotal(c.getSaleTotal())
                .claimReference(c.getClaimReference())
                .claimStatus(c.getClaimStatus() != null ? c.getClaimStatus().name() : null)
                .submittedAt(c.getSubmittedAt())
                .rejectionReason(c.getRejectionReason())
                .notes(c.getNotes())
                .build();
    }
}

