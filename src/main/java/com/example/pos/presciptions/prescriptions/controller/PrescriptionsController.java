package com.example.pos.presciptions.prescriptions.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.presciptions.prescriptions.dto.PrescriptionRequestDto;
import com.example.pos.presciptions.prescriptions.dto.PrescriptionResponseDto;
import com.example.pos.presciptions.prescriptions.model.Prescriptions;
import com.example.pos.presciptions.prescriptions.service.PrescriptionsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionsController {

    private final PrescriptionsService service;
    public PrescriptionsController(PrescriptionsService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionResponseDto>> create(@RequestBody @Valid PrescriptionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.toDto(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PrescriptionResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll().stream().map(service::toDto).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.toDto(service.getById(id))));
    }

    @PatchMapping("/{id}/dispense")
    public ResponseEntity<ApiResponse<PrescriptionResponseDto>> dispense(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.updated(service.toDto(service.dispense(id))));
    }
}
