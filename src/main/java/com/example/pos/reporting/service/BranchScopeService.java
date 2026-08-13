package com.example.pos.reporting.service;

import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.security.auth.UserDetailsImpl;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BranchScopeService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public BranchScopeService(UserRepository userRepository, BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    public List<Branch> resolve(UserDetailsImpl principal, UUID requestedBranchId, UUID requestedPharmacyId) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));
        Branch userBranch = user.getBranch();
        UUID userPharmacyId = userBranch != null && userBranch.getPharmacy() != null
                ? userBranch.getPharmacy().getId() : null;
        boolean owner = hasRole(principal, "OWNER");

        if (requestedPharmacyId != null && !requestedPharmacyId.equals(userPharmacyId)) {
            throw new ForbiddenException("You cannot access another pharmacy");
        }

        if (!owner) {
            if (userBranch == null) {
                throw new ForbiddenException("User has no assigned branch");
            }
            if (requestedBranchId != null && !requestedBranchId.equals(userBranch.getId())) {
                throw new ForbiddenException("You cannot access another branch");
            }
            return List.of(userBranch);
        }

        if (requestedBranchId != null) {
            Branch branch = branchRepository.findById(requestedBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", requestedBranchId));
            if (branch.getPharmacy() == null || !branch.getPharmacy().getId().equals(userPharmacyId)) {
                throw new ForbiddenException("Branch does not belong to your pharmacy");
            }
            return List.of(branch);
        }

        if (userPharmacyId == null) {
            throw new ForbiddenException("User has no assigned pharmacy");
        }
        return branchRepository.findByPharmacyId(userPharmacyId);
    }

    private boolean hasRole(UserDetailsImpl principal, String role) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }
}
