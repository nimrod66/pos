package com.example.pos.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsurerRequestDto {

    @NotBlank(message = "Insurer name is required")
    private String name;

    @NotBlank(message = "Insurer code is required")
    private String code;

    @NotBlank(message = "Insurer type is required")
    private String insurerType;

    private String contactPerson;
    private String phoneNumber;
    private String email;
    private String claimSubmissionEmail;
    private String preauthPhone;
    private BigDecimal defaultCoPayPercentage;
    private BigDecimal defaultCoPayFlat;
    private boolean requiresPreauth;
    private BigDecimal maxClaimAmount;
    private String status;
}
