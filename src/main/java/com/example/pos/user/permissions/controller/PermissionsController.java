package com.example.pos.user.permissions.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.user.permissions.dto.PermissionRequestDto;
import com.example.pos.user.permissions.dto.PermissionResponseDto;
import com.example.pos.user.permissions.model.Permissions;
import com.example.pos.user.permissions.service.PermissionsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionsController {

    private final PermissionsService permissionsService;

    public PermissionsController(PermissionsService permissionsService) {
        this.permissionsService = permissionsService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> create(
            @RequestBody @Valid PermissionRequestDto dto) {
        Permissions permission = permissionsService.createPermission(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(PermissionResponseDto.from(permission)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<List<PermissionResponseDto>>> getAll(
            @RequestParam(required = false) String module) {
        List<Permissions> permissions;
        if (module != null) {
            permissions = permissionsService.getPermissionsByModule(module);
        } else {
            permissions = permissionsService.getAllPermissions();
        }
        List<PermissionResponseDto> response = permissions.stream()
                .map(PermissionResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> getById(@PathVariable UUID id) {
        Permissions permission = permissionsService.getPermissionById(id);
        return ResponseEntity.ok(ApiResponse.ok(PermissionResponseDto.from(permission)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<PermissionResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid PermissionRequestDto dto) {
        Permissions permission = permissionsService.updatePermission(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(PermissionResponseDto.from(permission)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        permissionsService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
