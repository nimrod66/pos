package com.example.pos.user.roles.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.roles.dto.AssignPermissionsRequestDto;
import com.example.pos.user.roles.dto.UserRolesRequestDto;
import com.example.pos.user.roles.dto.UserRolesResponseDto;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.roles.service.UserRolesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class UserRolesController {

    private final UserRolesService rolesService;

    public UserRolesController(UserRolesService rolesService) {
        this.rolesService = rolesService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserRolesResponseDto>> create(@RequestBody @Valid UserRolesRequestDto dto) {
        UserRoles role = rolesService.createRole(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(UserRolesResponseDto.from(role)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserRolesResponseDto>>> getAll() {
        List<UserRolesResponseDto> roles = rolesService.getAllRoles().stream()
                .map(UserRolesResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(roles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRolesResponseDto>> getById(@PathVariable UUID id) {
        UserRoles role = rolesService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.ok(UserRolesResponseDto.from(role)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRolesResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UserRolesRequestDto dto) {
        UserRoles role = rolesService.updateRole(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(UserRolesResponseDto.from(role)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        rolesService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<Void>> assignPermissions(
            @PathVariable UUID id,
            @RequestBody @Valid AssignPermissionsRequestDto dto) {
        rolesService.assignPermissions(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(null));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<Void>> removePermission(
            @PathVariable UUID id,
            @PathVariable UUID permissionId) {
        rolesService.removePermission(id, permissionId);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
