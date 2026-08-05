package com.example.pos.insurance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceClaimRequestDto {

    @NotNull(message = "Sale ID is required")
    private UUID saleId;

    @NotNull(message = "Insurer ID is required")
    private UUID insurerId;

    private UUID schemeId;

    private UUID memberId;

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

    private UUID authorizationId;

    private String notes;
}

