package com.example.pos.user.staffshifts.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.user.staffshifts.dto.StaffShiftRequestDto;
import com.example.pos.user.staffshifts.dto.StaffShiftResponseDto;
import com.example.pos.user.staffshifts.dto.UpdateShiftStatusDto;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.service.StaffShiftsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts")
public class StaffShiftsController {

    private final StaffShiftsService shiftService;

    public StaffShiftsController(StaffShiftsService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('shift.open')")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> openShift(
            @RequestBody @Valid StaffShiftRequestDto dto) {
        StaffShifts shift = shiftService.openShift(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(shiftService.toResponse(shift)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('shift.open', 'shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<List<StaffShiftResponseDto>>> getAll(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID userId) {
        List<StaffShifts> shifts;
        if (userId != null) {
            shifts = shiftService.getShiftsByUser(userId);
        } else if (branchId != null) {
            shifts = shiftService.getShiftsByBranch(branchId);
        } else {
            shifts = List.of();
        }
        List<StaffShiftResponseDto> response = shifts.stream()
                .map(shiftService::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('shift.open', 'shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<List<StaffShiftResponseDto>>> getActive(
            @RequestParam(required = false) UUID branchId) {
        List<StaffShifts> shifts = shiftService.getActiveShifts(branchId);
        List<StaffShiftResponseDto> response = shifts.stream()
                .map(shiftService::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/active/user/{userId}")
    @PreAuthorize("hasAnyAuthority('shift.open', 'shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> getActiveByUser(
            @PathVariable UUID userId) {
        StaffShifts shift = shiftService.getActiveShiftForUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(shiftService.toResponse(shift)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('shift.open', 'shift.close', 'shift.variance.approve')")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> getById(@PathVariable UUID id) {
        StaffShifts shift = shiftService.getShiftById(id);
        return ResponseEntity.ok(ApiResponse.ok(shiftService.toResponse(shift)));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('shift.close')")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> closeShift(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateShiftStatusDto dto) {
        StaffShifts shift = shiftService.closeShift(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(shiftService.toResponse(shift)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('shift.variance.approve')")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> cancelShift(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateShiftStatusDto dto) {
        StaffShifts shift = shiftService.cancelShift(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(shiftService.toResponse(shift)));
    }
}
