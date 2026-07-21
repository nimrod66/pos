package com.example.pos.user.permissions.repository;

import com.example.pos.user.permissions.model.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionsRepository extends JpaRepository<Permissions, Long> {

    Optional<Permissions> findByPermissionName(String permissionName);

    List<Permissions> findByModuleName(String moduleName);

    boolean existsByPermissionName(String permissionName);

    boolean existsByPermissionNameAndIdNot(String permissionName, Long id);
}
