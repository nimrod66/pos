package com.example.pos.user.userbranchrole.dto;

import com.example.pos.user.userbranchrole.model.UserBranchRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranchRoleResponseDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private UUID branchId;
    private String branchName;
    private UUID roleId;
    private String roleName;
    private UUID assignedById;
    private String assignedByName;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;

    public static UserBranchRoleResponseDto from(UserBranchRole assignment) {
        return UserBranchRoleResponseDto.builder()
                .id(assignment.getId())
                .userId(assignment.getUser() != null ? assignment.getUser().getId() : null)
                .userName(assignment.getUser() != null
                        ? assignment.getUser().getFirstName() + " " + assignment.getUser().getLastName()
                        : null)
                .branchId(assignment.getBranch() != null ? assignment.getBranch().getId() : null)
                .branchName(assignment.getBranch() != null ? assignment.getBranch().getBranchName() : null)
                .roleId(assignment.getRole() != null ? assignment.getRole().getId() : null)
                .roleName(assignment.getRole() != null ? assignment.getRole().getRoleName() : null)
                .assignedById(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null)
                .assignedByName(assignment.getAssignedBy() != null
                        ? assignment.getAssignedBy().getFirstName() + " " + assignment.getAssignedBy().getLastName()
                        : null)
                .assignedAt(assignment.getAssignedAt())
                .createdAt(assignment.getCreatedAt())
                .build();
    }
}

