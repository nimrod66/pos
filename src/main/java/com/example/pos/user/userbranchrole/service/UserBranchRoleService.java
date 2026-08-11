package com.example.pos.user.userbranchrole.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.security.auth.AuthService;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.security.auth.PermissionCodes;
import com.example.pos.user.roles.model.UserRoles;
import com.example.pos.user.roles.repository.UserRolesRepository;
import com.example.pos.user.userbranchrole.dto.UserBranchRoleRequestDto;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import com.example.pos.user.userbranchrole.repository.UserBranchRoleRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserBranchRoleService {

    private final UserBranchRoleRepository branchRoleRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final UserRolesRepository rolesRepository;
    private final AuthenticatedUserContext current;
    private final AuthService authService;

    public UserBranchRoleService(UserBranchRoleRepository branchRoleRepository,
                                 UserRepository userRepository,
                                 BranchRepository branchRepository,
                                 UserRolesRepository rolesRepository,
                                 AuthenticatedUserContext current,
                                 AuthService authService) {
        this.branchRoleRepository = branchRoleRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.rolesRepository = rolesRepository;
        this.current = current;
        this.authService = authService;
    }

    public UserBranchRole assignRole(UserBranchRoleRequestDto dto) {
        UUID pharmacyId = current.pharmacy().getId();
        User actor = current.user();
        User user = userRepository.findByIdAndBranchPharmacyId(dto.getUserId(), pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));
        if (user.getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot change your own role assignments");
        }
        Branch branch = branchRepository.findByIdAndPharmacyId(dto.getBranchId(), pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        if (!user.getBranch().getId().equals(branch.getId())) {
            throw new ConflictException("Role branch must match the user's active branch",
                    "USER_BRANCH_MISMATCH");
        }
        UserRoles role = rolesRepository.findById(dto.getRoleId())
                .filter(value -> PermissionCodes.ROLE_BUNDLES.containsKey(value.getRoleName()))
                .orElseThrow(() -> new ResourceNotFoundException("Canonical role", dto.getRoleId()));

        if (branchRoleRepository.existsByUserIdAndBranchIdAndRoleId(
                user.getId(), branch.getId(), role.getId())) {
            throw new ConflictException("User already has this role in this branch");
        }
        UserBranchRole assignment = UserBranchRole.builder()
                .user(user)
                .branch(branch)
                .role(role)
                .assignedBy(actor)
                .assignedAt(LocalDateTime.now())
                .build();
        UserBranchRole saved = branchRoleRepository.save(assignment);
        authService.revokeUserSessions(user.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<UserBranchRole> getAssignmentsByUser(UUID userId) {
        userRepository.findByIdAndBranchPharmacyId(userId, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return branchRoleRepository.findByUserIdAndBranchPharmacyId(
                userId, current.pharmacy().getId());
    }

    @Transactional(readOnly = true)
    public List<UserBranchRole> getAssignmentsByBranch(UUID branchId) {
        branchRepository.findByIdAndPharmacyId(branchId, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        return branchRoleRepository.findByBranchIdAndBranchPharmacyId(
                branchId, current.pharmacy().getId());
    }

    @Transactional(readOnly = true)
    public List<UserBranchRole> getAssignmentsByUserAndBranch(UUID userId, UUID branchId) {
        return branchRoleRepository.findByUserIdAndBranchIdAndBranchPharmacyId(
                userId, branchId, current.pharmacy().getId());
    }

    public void removeAssignment(UUID id) {
        User actor = current.user();
        UserBranchRole assignment = branchRoleRepository.findDetailedByIdAndBranchPharmacyId(
                        id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("UserBranchRole", id));
        if (assignment.getUser().getId().equals(actor.getId())) {
            throw new ForbiddenException("You cannot remove your own role assignment");
        }
        if ("OWNER".equals(assignment.getRole().getRoleName())
                && branchRoleRepository.countActiveOwners(current.pharmacy().getId()) <= 1) {
            throw new ConflictException("A pharmacy must retain at least one active owner",
                    "FINAL_OWNER_REQUIRED");
        }
        UUID affectedUserId = assignment.getUser().getId();
        branchRoleRepository.delete(assignment);
        authService.revokeUserSessions(affectedUserId);
    }
}
