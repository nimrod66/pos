package com.example.pos.user.userbranchrole.repository;

import java.util.UUID;

import com.example.pos.user.userbranchrole.model.UserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBranchRoleRepository extends JpaRepository<UserBranchRole, UUID> {

    @EntityGraph(attributePaths = {"user", "branch", "branch.pharmacy", "role", "assignedBy"})
    java.util.Optional<UserBranchRole> findDetailedByIdAndBranchPharmacyId(UUID id, UUID pharmacyId);

    @EntityGraph(attributePaths = {"user", "branch", "branch.pharmacy", "role", "assignedBy"})
    List<UserBranchRole> findByUserIdAndBranchPharmacyId(UUID userId, UUID pharmacyId);

    @EntityGraph(attributePaths = {"user", "branch", "branch.pharmacy", "role", "assignedBy"})
    List<UserBranchRole> findByBranchIdAndBranchPharmacyId(UUID branchId, UUID pharmacyId);

    @EntityGraph(attributePaths = {"user", "branch", "branch.pharmacy", "role", "assignedBy"})
    List<UserBranchRole> findByUserIdAndBranchIdAndBranchPharmacyId(
            UUID userId, UUID branchId, UUID pharmacyId);

    @Query("select count(distinct assignment.user.id) from UserBranchRole assignment "
            + "where assignment.branch.pharmacy.id = :pharmacyId "
            + "and assignment.role.roleName = 'OWNER' "
            + "and assignment.user.status = com.example.pos.user.users.model.User.Status.ACTIVE")
    long countActiveOwners(@Param("pharmacyId") UUID pharmacyId);

    boolean existsByUserIdAndRoleRoleName(UUID userId, String roleName);

    List<UserBranchRole> findByUserId(UUID userId);

    List<UserBranchRole> findByBranchId(UUID branchId);

    List<UserBranchRole> findByUserIdAndBranchId(UUID userId, UUID branchId);

    boolean existsByUserIdAndBranchIdAndRoleId(UUID userId, UUID branchId, UUID roleId);
}
