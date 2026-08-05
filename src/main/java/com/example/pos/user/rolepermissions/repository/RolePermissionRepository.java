package com.example.pos.user.rolepermissions.repository;

import java.util.UUID;

import com.example.pos.user.rolepermissions.model.RolePermission;
import com.example.pos.user.roles.model.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByUserRoles(UserRoles role);

    Optional<RolePermission> findByUserRolesAndPermissionsId(UserRoles role, UUID permissionId);

    void deleteByUserRoles(UserRoles role);
}
