package com.example.pos.user.userbranchrole.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.user.userbranchrole.dto.UserBranchRoleRequestDto;
import com.example.pos.user.userbranchrole.dto.UserBranchRoleResponseDto;
import com.example.pos.user.userbranchrole.model.UserBranchRole;
import com.example.pos.user.userbranchrole.service.UserBranchRoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-branch-roles")
public class UserBranchRoleController {

    private final UserBranchRoleService branchRoleService;

    public UserBranchRoleController(UserBranchRoleService branchRoleService) {
        this.branchRoleService = branchRoleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserBranchRoleResponseDto>> assign(
            @RequestBody @Valid UserBranchRoleRequestDto dto,
            @RequestAttribute(required = false) UUID currentUserId) {
        UUID assignedBy = currentUserId != null ? currentUserId : UUID.randomUUID();
        UserBranchRole assignment = branchRoleService.assignRole(dto, assignedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(UserBranchRoleResponseDto.from(assignment)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserBranchRoleResponseDto>>> getAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID branchId) {
        List<UserBranchRole> assignments;
        if (userId != null && branchId != null) {
            assignments = branchRoleService.getAssignmentsByUserAndBranch(userId, branchId);
        } else if (userId != null) {
            assignments = branchRoleService.getAssignmentsByUser(userId);
        } else if (branchId != null) {
            assignments = branchRoleService.getAssignmentsByBranch(branchId);
        } else {
            assignments = List.of();
        }
        List<UserBranchRoleResponseDto> response = assignments.stream()
                .map(UserBranchRoleResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID id) {
        branchRoleService.removeAssignment(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
