package com.example.pos.user.users.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.repository.BranchRepository;
import com.example.pos.notification.email.EmailService;
import com.example.pos.user.users.dto.ChangePasswordRequestDto;
import com.example.pos.user.users.dto.UpdateStatusRequestDto;
import com.example.pos.user.users.dto.UserRequestDto;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       BranchRepository branchRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User createUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email " + dto.getEmail() + " is already registered");
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));

        User user = new User();
        user.setBranch(branch);
        mapToEntity(dto, user);
        String rawPassword = dto.getPassword();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(User.Status.ACTIVE);

        user = userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), user.getEmail(), rawPassword);

        return user;
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByBranch(UUID branchId, Pageable pageable) {
        List<User> users = userRepository.findByBranchId(branchId);
        return new PageImpl<>(users, pageable, users.size());
    }

    @Transactional(readOnly = true)
    public Page<User> search(String q, Pageable pageable) {
        return userRepository.search(q, pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email));
    }

    public User updateUser(UUID id, UserRequestDto dto) {
        User user = getUserById(id);

        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ConflictException("Email " + dto.getEmail() + " is already in use");
        }

        if (!user.getBranch().getId().equals(dto.getBranchId())) {
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", dto.getBranchId()));
            user.setBranch(branch);
        }

        mapToEntity(dto, user);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return userRepository.save(user);
    }

    public User updateStatus(UUID id, UpdateStatusRequestDto dto) {
        User user = getUserById(id);
        try {
            user.setStatus(User.Status.valueOf(dto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + dto.getStatus());
        }
        return userRepository.save(user);
    }

    public void changePassword(UUID id, ChangePasswordRequestDto dto) {
        User user = getUserById(id);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    private void mapToEntity(UserRequestDto dto, User user) {
        user.setFirstName(dto.getFirstName());
        user.setMiddleName(dto.getMiddleName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setEmail(dto.getEmail());
        if (dto.getStatus() != null) {
            try {
                user.setStatus(User.Status.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + dto.getStatus());
            }
        }
    }
}
