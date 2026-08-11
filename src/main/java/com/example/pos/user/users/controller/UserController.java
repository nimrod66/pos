package com.example.pos.user.users.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.user.users.dto.ChangePasswordRequestDto;
import com.example.pos.user.users.dto.UpdateStatusRequestDto;
import com.example.pos.user.users.dto.UserRequestDto;
import com.example.pos.user.users.dto.UserResponseDto;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<UserResponseDto>> create(@RequestBody @Valid UserRequestDto dto) {
        User user = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(UserResponseDto.from(user)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID branchId) {
        Page<User> page;
        if (branchId != null) {
            page = userService.getUsersByBranch(branchId, pageable);
        } else {
            page = userService.getAllUsers(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, UserResponseDto::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(UserResponseDto.from(user)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> page = userService.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, UserResponseDto::from)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UserRequestDto dto) {
        User user = userService.updateUser(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(UserResponseDto.from(user)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateStatusRequestDto dto) {
        User user = userService.updateStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(UserResponseDto.from(user)));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @RequestBody @Valid ChangePasswordRequestDto dto) {
        userService.changePassword(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user.manage')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
