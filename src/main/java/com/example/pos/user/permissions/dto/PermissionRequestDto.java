package com.example.pos.user.permissions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequestDto {

    @NotBlank(message = "Permission name is required")
    private String permissionName;

    @NotBlank(message = "Module name is required")
    private String moduleName;

    @NotBlank(message = "Action name is required")
    private String actionName;

    private String description;
}
