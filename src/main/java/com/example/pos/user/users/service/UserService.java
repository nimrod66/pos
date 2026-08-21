package com.example.pos.user.users.service;

import com.example.pos.common.annotation.Auditable;
import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ForbiddenException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.security.auth.AuthService;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.userbranchrole.repository.UserBranchRoleRepository;
import com.example.pos.user.users.dto.ChangePasswordRequestDto;
import com.example.pos.user.users.dto.UpdateStatusRequestDto;
import com.example.pos.user.users.dto.UserRequestDto;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final UserBranchRoleRepository branchRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserContext current;
    private final AuthService authService;

    public UserService(UserRepository userRepository,
                       BranchRepository branchRepository,
                       UserBranchRoleRepository branchRoleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticatedUserContext current,
                       AuthService authService) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.branchRoleRepository = branchRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.current = current;
        this.authService = authService;
    }

    @Auditable(action = "CREATE_USER", entity = "User")
    public User createUser(UserRequestDto dto) {
        String email = normalizeEmail(dto.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already registered");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        Branch branch = scopedBranch(dto.getBranchId());
        User user = new User();
        user.setBranch(branch);
        mapProfile(dto, user);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(User.Status.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findByBranchPharmacyId(current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByBranch(UUID branchId, Pageable pageable) {
        scopedBranch(branchId);
        return userRepository.findByBranchIdAndBranchPharmacyId(
                branchId, current.pharmacy().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> search(String q, Pageable pageable) {
        return userRepository.searchByPharmacy(current.pharmacy().getId(), q.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findByIdAndBranchPharmacyId(id, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email));
        if (!user.getBranch().getPharmacy().getId().equals(current.pharmacy().getId())) {
            throw new ResourceNotFoundException("User with email " + email);
        }
        return user;
    }

    @Auditable(action = "UPDATE_USER", entity = "User")
    public User updateUser(UUID id, UserRequestDto dto) {
        User user = getUserById(id);
        String email = normalizeEmail(dto.getEmail());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email " + email + " is already in use");
        }
        Branch branch = scopedBranch(dto.getBranchId());
        user.setBranch(branch);
        mapProfile(dto, user);
        user.setEmail(email);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            authService.revokeUserSessions(user.getId());
        }
        return userRepository.save(user);
    }

    @Auditable(action = "UPDATE_USER_STATUS", entity = "User")
    public User updateStatus(UUID id, UpdateStatusRequestDto dto) {
        User actor = current.user();
        User user = getUserById(id);
        if (actor.getId().equals(user.getId())) {
            throw new ForbiddenException("You cannot change your own account status");
        }
        User.Status status;
        try {
            status = User.Status.valueOf(dto.getStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + dto.getStatus());
        }
        if (status != User.Status.ACTIVE) ensureNotFinalOwner(user);
        user.setStatus(status);
        User saved = userRepository.save(user);
        if (status != User.Status.ACTIVE) authService.revokeUserSessions(user.getId());
        return saved;
    }

    @Auditable(action = "CHANGE_PASSWORD", entity = "User")
    public void changePassword(UUID id, ChangePasswordRequestDto dto) {
        if (!current.userId().equals(id)) {
            throw new ForbiddenException("You can only change your own password");
        }
        User user = getUserById(id);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must differ from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        authService.revokeUserSessions(user.getId());
    }

    @Auditable(action = "DEACTIVATE_USER", entity = "User")
    public void deleteUser(UUID id) {
        User actor = current.user();
        User user = getUserById(id);
        if (actor.getId().equals(user.getId())) {
            throw new ForbiddenException("You cannot deactivate your own account");
        }
        ensureNotFinalOwner(user);
        user.setStatus(User.Status.INACTIVE);
        userRepository.save(user);
        authService.revokeUserSessions(user.getId());
    }

    private void ensureNotFinalOwner(User user) {
        if (branchRoleRepository.existsByUserIdAndRoleRoleName(user.getId(), "OWNER")
                && branchRoleRepository.countActiveOwners(current.pharmacy().getId()) <= 1) {
            throw new ConflictException("A pharmacy must retain at least one active owner",
                    "FINAL_OWNER_REQUIRED");
        }
    }

    private Branch scopedBranch(UUID branchId) {
        return branchRepository.findByIdAndPharmacyId(branchId, current.pharmacy().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }

    private void mapProfile(UserRequestDto dto, User user) {
        user.setFirstName(dto.getFirstName().trim());
        user.setMiddleName(trimToNull(dto.getMiddleName()));
        user.setLastName(dto.getLastName().trim());
        user.setPhoneNumber(dto.getPhoneNumber().trim());
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
