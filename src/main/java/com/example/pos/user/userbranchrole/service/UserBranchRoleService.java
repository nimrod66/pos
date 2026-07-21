package com.example.pos.user.userbranchrole.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
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

@Service
@Transactional
public class UserBranchRoleService {

    private final UserBranchRoleRepository branchRoleRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final UserRolesRepository rolesRepository;

    public UserBranchRoleService(UserBranchRoleRepository branchRoleRepository,
                                 UserRepository userRepository,
                                 BranchRepository branchRepository,
                                 UserRolesRepository rolesRepository) {
        this.branchRoleRepository = branchRoleRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.rolesRepository = rolesRepository;
    }

    public UserBranchRole assignRole(UserBranchRoleRequestDto dto, Long assignedByUserId) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
        UserRoles role = rolesRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("UserRoles", dto.getRoleId()));

        if (branchRoleRepository.existsByUserIdAndBranchIdAndRoleId(
                dto.getUserId(), dto.getBranchId(), dto.getRoleId())) {
            throw new ConflictException("User already has this role in this branch");
        }

        User assignedBy = userRepository.findById(assignedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigning user", assignedByUserId));

        UserBranchRole assignment = new UserBranchRole();
        assignment.setUser(user);
        assignment.setBranch(branch);
        assignment.setRole(role);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(LocalDateTime.now());

        return branchRoleRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<UserBranchRole> getAssignmentsByUser(Long userId) {
        return branchRoleRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<UserBranchRole> getAssignmentsByBranch(Long branchId) {
        return branchRoleRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public List<UserBranchRole> getAssignmentsByUserAndBranch(Long userId, Long branchId) {
        return branchRoleRepository.findByUserIdAndBranchId(userId, branchId);
    }

    public void removeAssignment(Long id) {
        UserBranchRole assignment = branchRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserBranchRole", id));
        branchRoleRepository.delete(assignment);
    }
}
