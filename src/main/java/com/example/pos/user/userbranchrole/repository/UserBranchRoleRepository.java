package com.example.pos.user.userbranchrole.repository;

import com.example.pos.user.userbranchrole.model.UserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBranchRoleRepository extends JpaRepository<UserBranchRole, Long> {

    List<UserBranchRole> findByUserId(Long userId);

    List<UserBranchRole> findByBranchId(Long branchId);

    List<UserBranchRole> findByUserIdAndBranchId(Long userId, Long branchId);

    boolean existsByUserIdAndBranchIdAndRoleId(Long userId, Long branchId, Long roleId);
}
