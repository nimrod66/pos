package com.example.pos.user.userbranchrole.repository;

import java.util.UUID;

import com.example.pos.user.userbranchrole.model.UserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBranchRoleRepository extends JpaRepository<UserBranchRole, UUID> {

    List<UserBranchRole> findByUserId(UUID userId);

    List<UserBranchRole> findByBranchId(UUID branchId);

    List<UserBranchRole> findByUserIdAndBranchId(UUID userId, UUID branchId);

    boolean existsByUserIdAndBranchIdAndRoleId(UUID userId, UUID branchId, UUID roleId);
}
