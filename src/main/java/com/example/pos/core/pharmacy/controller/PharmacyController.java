package com.example.pos.core.pharmacy.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.pharmacy.dto.PharmacyRequestDto;
import com.example.pos.core.pharmacy.dto.PharmacyResponseDto;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.core.pharmacy.service.PharmacyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pharmacies")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PharmacyResponseDto>> create(@RequestBody @Valid PharmacyRequestDto dto) {
        Pharmacy pharmacy = pharmacyService.createPharmacy(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(PharmacyResponseDto.from(pharmacy)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PharmacyResponseDto>>> getAll() {
        List<PharmacyResponseDto> pharmacies = pharmacyService.getAllPharmacies()
                .stream()
                .map(PharmacyResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(pharmacies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PharmacyResponseDto>> getById(@PathVariable UUID id) {
        Pharmacy pharmacy = pharmacyService.getPharmacyById(id);
        return ResponseEntity.ok(ApiResponse.ok(PharmacyResponseDto.from(pharmacy)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PharmacyResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid PharmacyRequestDto dto) {
        Pharmacy pharmacy = pharmacyService.updatePharmacy(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(PharmacyResponseDto.from(pharmacy)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        pharmacyService.deletePharmacy(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
