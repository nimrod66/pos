package com.example.pos.insurance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceClaimRequestDto {

    @NotNull(message = "Sale ID is required")
    private Long saleId;

    @NotNull(message = "Insurer ID is required")
    private Long insurerId;

    private Long schemeId;

    private Long memberId;

    private String patientName;

    private String patientMembershipId;

    @NotNull(message = "Claim amount is required")
    @Positive(message = "Claim amount must be positive")
    private BigDecimal claimAmount;

    @NotNull(message = "Sale total is required")
    @Positive(message = "Sale total must be positive")
    private BigDecimal saleTotal;

    private BigDecimal coPayAmount;

    private BigDecimal approvedAmount;

    private BigDecimal rejectedAmount;

    private Long authorizationId;

    private String notes;
}
