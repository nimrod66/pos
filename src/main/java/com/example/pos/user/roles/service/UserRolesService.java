package com.example.pos.user.roles.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
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
        if (rolesRepository.existsByRoleName(dto.getRoleName())) {
            throw new ConflictException("Role '" + dto.getRoleName() + "' already exists");
        }
        UserRoles role = new UserRoles();
        role.setRoleName(dto.getRoleName());
        return rolesRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<UserRoles> getAllRoles() {
        return rolesRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserRoles getRoleById(Long id) {
        return rolesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserRoles", id));
    }

    @Transactional(readOnly = true)
    public UserRoles getRoleByName(String roleName) {
        return rolesRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("UserRoles with name " + roleName));
    }

    public UserRoles updateRole(Long id, UserRolesRequestDto dto) {
        UserRoles role = getRoleById(id);
        if (rolesRepository.existsByRoleNameAndIdNot(dto.getRoleName(), id)) {
            throw new ConflictException("Role '" + dto.getRoleName() + "' already exists");
        }
        role.setRoleName(dto.getRoleName());
        return rolesRepository.save(role);
    }

    public void deleteRole(Long id) {
        UserRoles role = getRoleById(id);
        rolesRepository.delete(role);
    }

    public List<RolePermission> assignPermissions(Long roleId, AssignPermissionsRequestDto dto) {
        UserRoles role = getRoleById(roleId);
        List<Permissions> permissions = permissionsRepository.findAllById(dto.getPermissionIds());

        if (permissions.size() != dto.getPermissionIds().size()) {
            throw new ResourceNotFoundException("One or more permissions not found");
        }

        List<RolePermission> assignments = permissions.stream()
                .map(permission -> {
                    RolePermission rp = new RolePermission();
                    rp.setUserRoles(role);
                    rp.setPermissions(permission);
                    return rp;
                })
                .toList();

        return rolePermissionRepository.saveAll(assignments);
    }

    public void removePermission(Long roleId, Long permissionId) {
        UserRoles role = getRoleById(roleId);
        RolePermission assignment = rolePermissionRepository.findByUserRolesAndPermissionsId(role, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RolePermission assignment for role " + roleId + " and permission " + permissionId));
        rolePermissionRepository.delete(assignment);
    }
}
