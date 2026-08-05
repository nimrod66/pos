package com.example.pos.user.roles.repository;

import java.util.UUID;

import com.example.pos.user.roles.model.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRolesRepository extends JpaRepository<UserRoles, UUID> {

    Optional<UserRoles> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    boolean existsByRoleNameAndIdNot(String roleName, UUID id);
}
