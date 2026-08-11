package com.example.pos.security.auth;

import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.UnauthorizedException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedUserContext {

    private final UserRepository userRepository;

    public AuthenticatedUserContext(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetailsImpl principal)) {
            throw new UnauthorizedException("Authentication is required");
        }
        return principal.getUserId();
    }

    public User user() {
        return userRepository.findContextById(userId())
                .filter(user -> user.getStatus() == User.Status.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("The authenticated account is unavailable"));
    }

    public Branch branch() {
        Branch branch = user().getBranch();
        if (branch == null || branch.getStatus() != Branch.Status.ACTIVE) {
            throw new ForbiddenException("The account has no active branch");
        }
        return branch;
    }

    public UUID branchId() {
        return branch().getId();
    }

    public Pharmacy pharmacy() {
        Pharmacy pharmacy = branch().getPharmacy();
        if (pharmacy == null) {
            throw new ForbiddenException("The account has no pharmacy context");
        }
        return pharmacy;
    }

    public UUID pharmacyId() {
        return pharmacy().getId();
    }

    public void requireBranch(UUID branchId) {
        if (branchId == null || !branch().getId().equals(branchId)) {
            throw new ForbiddenException("The requested branch is outside the active session");
        }
    }

    public void requirePharmacy(UUID pharmacyId) {
        if (pharmacyId == null || !pharmacy().getId().equals(pharmacyId)) {
            throw new ForbiddenException("The requested pharmacy is outside the active session");
        }
    }

    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
