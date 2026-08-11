package com.example.pos.user.permissions.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.security.auth.PermissionCodes;
import com.example.pos.user.permissions.dto.PermissionRequestDto;
import com.example.pos.user.permissions.model.Permissions;
import com.example.pos.user.permissions.repository.PermissionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PermissionsService {

    private final PermissionsRepository permissionsRepository;

    public PermissionsService(PermissionsRepository permissionsRepository) {
        this.permissionsRepository = permissionsRepository;
    }

    public Permissions createPermission(PermissionRequestDto dto) {
        throw fixedPermissions();
    }

    @Transactional(readOnly = true)
    public List<Permissions> getAllPermissions() {
        return permissionsRepository.findAll().stream()
                .filter(permission -> PermissionCodes.ALL.contains(permission.getPermissionName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Permissions> getPermissionsByModule(String moduleName) {
        return permissionsRepository.findByModuleName(moduleName).stream()
                .filter(permission -> PermissionCodes.ALL.contains(permission.getPermissionName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Permissions getPermissionById(UUID id) {
        return permissionsRepository.findById(id)
                .filter(permission -> PermissionCodes.ALL.contains(permission.getPermissionName()))
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    public Permissions updatePermission(UUID id, PermissionRequestDto dto) {
        throw fixedPermissions();
    }

    public void deletePermission(UUID id) {
        throw fixedPermissions();
    }

    private void mapToEntity(PermissionRequestDto dto, Permissions permission) {
        permission.setPermissionName(dto.getPermissionName());
        permission.setModuleName(dto.getModuleName());
        permission.setActionName(dto.getActionName());
        permission.setDescription(dto.getDescription());
    }

    private ForbiddenException fixedPermissions() {
        return new ForbiddenException("Permissions are fixed by the shared POS specification");
    }
}
