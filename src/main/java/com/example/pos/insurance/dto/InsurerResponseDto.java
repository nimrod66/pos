package com.example.pos.insurance.dto;

import com.example.pos.insurance.model.Insurer;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurerResponseDto {
    private UUID id;
    private String name;
    private String code;
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

    public static InsurerResponseDto from(Insurer i) {
        return InsurerResponseDto.builder()
                .id(i.getId())
                .name(i.getName())
                .code(i.getCode())
                .insurerType(i.getInsurerType() != null ? i.getInsurerType().name() : null)
                .contactPerson(i.getContactPerson())
                .phoneNumber(i.getPhoneNumber())
                .email(i.getEmail())
                .claimSubmissionEmail(i.getClaimSubmissionEmail())
                .preauthPhone(i.getPreauthPhone())
                .defaultCoPayPercentage(i.getDefaultCoPayPercentage())
                .defaultCoPayFlat(i.getDefaultCoPayFlat())
                .requiresPreauth(i.isRequiresPreauth())
                .maxClaimAmount(i.getMaxClaimAmount())
                .status(i.getStatus() != null ? i.getStatus().name() : null)
                .build();
    }
}

