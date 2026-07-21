package com.example.pos.user.roles.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionsRequestDto {

    @NotNull(message = "Permission IDs are required")
    private List<Long> permissionIds;
}
