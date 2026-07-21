package com.example.pos.user.roles.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRolesRequestDto {

    @NotBlank(message = "Role name is required")
    private String roleName;
}
