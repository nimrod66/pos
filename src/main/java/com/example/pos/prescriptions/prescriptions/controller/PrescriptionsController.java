package com.example.pos.prescriptions.prescriptions.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.prescriptions.prescriptions.dto.PrescriptionRequestDto;
import com.example.pos.prescriptions.prescriptions.dto.PrescriptionResponseDto;
import com.example.pos.prescriptions.prescriptions.model.Prescriptions;
import com.example.pos.prescriptions.prescriptions.service.PrescriptionsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionsController {

    private final PrescriptionsService service;
    public PrescriptionsController(PrescriptionsService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('prescription.approve')")
    public ResponseEntity<ApiResponse<PrescriptionResponseDto>> create(@RequestBody @Valid PrescriptionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.toDto(service.create(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('prescription.read')")
    public ResponseEntity<ApiResponse<PagedResponse<PrescriptionResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Prescriptions> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, service::toDto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('prescription.read')")
    public ResponseEntity<ApiResponse<PrescriptionResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.toDto(service.getById(id))));
    }

    @PatchMapping("/{id}/dispense")
    @PreAuthorize("hasAuthority('prescription.approve')")
    public ResponseEntity<ApiResponse<PrescriptionResponseDto>> dispense(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.updated(service.toDto(service.dispense(id))));
    }
}
