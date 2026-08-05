package com.example.pos.core.branch.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.branch.dto.BranchRequestDto;
import com.example.pos.core.branch.dto.BranchResponseDto;
import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.branch.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponseDto>> create(@RequestBody @Valid BranchRequestDto dto) {
        Branch branch = branchService.createBranch(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(BranchResponseDto.from(branch)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponseDto>>> getAll(
            @RequestParam(required = false) UUID pharmacyId) {
        List<Branch> branches;
        if (pharmacyId != null) {
            branches = branchService.getBranchesByPharmacyId(pharmacyId);
        } else {
            branches = branchService.getAllBranches();
        }
        List<BranchResponseDto> response = branches.stream()
                .map(BranchResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponseDto>> getById(@PathVariable UUID id) {
        Branch branch = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.ok(BranchResponseDto.from(branch)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid BranchRequestDto dto) {
        Branch branch = branchService.updateBranch(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(BranchResponseDto.from(branch)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
