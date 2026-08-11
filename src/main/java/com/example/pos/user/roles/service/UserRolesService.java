package com.example.pos.user.roles.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.security.auth.PermissionCodes;
import com.example.pos.user.permissions.model.Permissions;
import com.example.pos.user.permissions.repository.PermissionsRepository;
import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.rolepermissions.repository.RolePermissionRepository;
import com.example.pos.user.roles.dto.AssignPermissionsRequestDto;
import com.example.pos.user.roles.dto.UserRolesRequestDto;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.roles.repository.UserRolesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserRolesService {

    private final UserRolesRepository rolesRepository;
    private final PermissionsRepository permissionsRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public UserRolesService(UserRolesRepository rolesRepository,
                            PermissionsRepository permissionsRepository,
                            RolePermissionRepository rolePermissionRepository) {
        this.rolesRepository = rolesRepository;
        this.permissionsRepository = permissionsRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public UserRoles createRole(UserRolesRequestDto dto) {
        throw canonicalRolesOnly();
    }

    @Transactional(readOnly = true)
    public List<UserRoles> getAllRoles() {
        return rolesRepository.findAll().stream()
                .filter(role -> PermissionCodes.ROLE_BUNDLES.containsKey(role.getRoleName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserRoles getRoleById(UUID id) {
        return rolesRepository.findById(id)
                .filter(role -> PermissionCodes.ROLE_BUNDLES.containsKey(role.getRoleName()))
                .orElseThrow(() -> new ResourceNotFoundException("UserRoles", id));
    }

    @Transactional(readOnly = true)
    public UserRoles getRoleByName(String roleName) {
        return rolesRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("UserRoles with name " + roleName));
    }

    public UserRoles updateRole(UUID id, UserRolesRequestDto dto) {
        throw canonicalRolesOnly();
    }

    public void deleteRole(UUID id) {
        throw canonicalRolesOnly();
    }

    public List<RolePermission> assignPermissions(UUID roleId, AssignPermissionsRequestDto dto) {
        throw canonicalRolesOnly();
    }

    public void removePermission(UUID roleId, UUID permissionId) {
        throw canonicalRolesOnly();
    }

    private ForbiddenException canonicalRolesOnly() {
        return new ForbiddenException("Roles and permission bundles are fixed by the shared POS specification");
    }
}
