package com.example.pos.user.roles.repository;

import com.example.pos.user.roles.model.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRolesRepository extends JpaRepository<UserRoles, Long> {

    Optional<UserRoles> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    boolean existsByRoleNameAndIdNot(String roleName, Long id);
}
