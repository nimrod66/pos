package com.example.pos.user.permissions.dto;

import com.example.pos.user.permissions.model.Permissions;
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
public class PermissionResponseDto {

    private UUID id;
    private String permissionName;
    private String moduleName;
    private String actionName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PermissionResponseDto from(Permissions permission) {
        return PermissionResponseDto.builder()
                .id(permission.getId())
                .permissionName(permission.getPermissionName())
                .moduleName(permission.getModuleName())
                .actionName(permission.getActionName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}

