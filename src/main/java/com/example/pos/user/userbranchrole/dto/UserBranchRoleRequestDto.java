package com.example.pos.user.userbranchrole.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBranchRoleRequestDto {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    @NotNull(message = "Role ID is required")
    private UUID roleId;
}

