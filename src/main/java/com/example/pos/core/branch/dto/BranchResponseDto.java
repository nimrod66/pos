package com.example.pos.core.branch.dto;

import com.example.pos.core.branch.model.Branch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponseDto {

    private Long id;
    private String branchName;
    private String branchCode;
    private String phoneNumber;
    private String email;
    private String location;
    private String status;
    private Long pharmacyId;
    private String pharmacyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BranchResponseDto from(Branch branch) {
        return BranchResponseDto.builder()
                .id(branch.getId())
                .branchName(branch.getBranchName())
                .branchCode(branch.getBranchCode())
                .phoneNumber(branch.getPhoneNumber())
                .email(branch.getEmail())
                .location(branch.getLocation())
                .status(branch.getStatus() != null ? branch.getStatus().name() : null)
                .pharmacyId(branch.getPharmacy() != null ? branch.getPharmacy().getId() : null)
                .pharmacyName(branch.getPharmacy() != null ? branch.getPharmacy().getName() : null)
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}
