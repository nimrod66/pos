package com.example.pos.user.staffshifts.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.user.staffshifts.dto.StaffShiftRequestDto;
import com.example.pos.user.staffshifts.dto.StaffShiftResponseDto;
import com.example.pos.user.staffshifts.dto.UpdateShiftStatusDto;
import com.example.pos.user.staffshifts.model.StaffShifts;
import com.example.pos.user.staffshifts.service.StaffShiftsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class StaffShiftsController {

    private final StaffShiftsService shiftService;

    public StaffShiftsController(StaffShiftsService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> openShift(
            @RequestBody @Valid StaffShiftRequestDto dto) {
        StaffShifts shift = shiftService.openShift(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(StaffShiftResponseDto.from(shift)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffShiftResponseDto>>> getAll(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long userId) {
        List<StaffShifts> shifts;
        if (userId != null) {
            shifts = shiftService.getShiftsByUser(userId);
        } else if (branchId != null) {
            shifts = shiftService.getShiftsByBranch(branchId);
        } else {
            shifts = List.of();
        }
        List<StaffShiftResponseDto> response = shifts.stream()
                .map(StaffShiftResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<StaffShiftResponseDto>>> getActive(
            @RequestParam(required = false) Long branchId) {
        List<StaffShifts> shifts = shiftService.getActiveShifts(branchId);
        List<StaffShiftResponseDto> response = shifts.stream()
                .map(StaffShiftResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/active/user/{userId}")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> getActiveByUser(
            @PathVariable Long userId) {
        StaffShifts shift = shiftService.getActiveShiftForUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(StaffShiftResponseDto.from(shift)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> getById(@PathVariable Long id) {
        StaffShifts shift = shiftService.getShiftById(id);
        return ResponseEntity.ok(ApiResponse.ok(StaffShiftResponseDto.from(shift)));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> closeShift(
            @PathVariable Long id,
            @RequestBody @Valid UpdateShiftStatusDto dto) {
        StaffShifts shift = shiftService.closeShift(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(StaffShiftResponseDto.from(shift)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<StaffShiftResponseDto>> cancelShift(
            @PathVariable Long id,
            @RequestBody @Valid UpdateShiftStatusDto dto) {
        StaffShifts shift = shiftService.cancelShift(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(StaffShiftResponseDto.from(shift)));
    }
}
