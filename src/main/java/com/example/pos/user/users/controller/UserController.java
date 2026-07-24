package com.example.pos.user.users.controller;

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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> create(@RequestBody @Valid UserRequestDto dto) {
        User user = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(UserResponseDto.from(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long branchId) {
        Page<User> page;
        if (branchId != null) {
            page = userService.getUsersByBranch(branchId, pageable);
        } else {
            page = userService.getAllUsers(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, UserResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(UserResponseDto.from(user)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> page = userService.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, UserResponseDto::from)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UserRequestDto dto) {
        User user = userService.updateUser(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(UserResponseDto.from(user)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatusRequestDto dto) {
        User user = userService.updateStatus(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(UserResponseDto.from(user)));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @RequestBody @Valid ChangePasswordRequestDto dto) {
        userService.changePassword(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
