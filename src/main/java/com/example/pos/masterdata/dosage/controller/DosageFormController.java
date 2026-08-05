package com.example.pos.masterdata.dosage.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.masterdata.dosage.dto.DosageFormRequestDto;
import com.example.pos.masterdata.dosage.dto.DosageFormResponseDto;
import com.example.pos.masterdata.dosage.model.DosageForm;
import com.example.pos.masterdata.dosage.service.DosageFormService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dosage-forms")
public class DosageFormController {

    private final DosageFormService service;

    public DosageFormController(DosageFormService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<DosageFormResponseDto>> create(@RequestBody @Valid DosageFormRequestDto dto) {
        DosageForm form = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(DosageFormResponseDto.from(form)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DosageFormResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll().stream().map(DosageFormResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DosageFormResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(DosageFormResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DosageFormResponseDto>> update(@PathVariable UUID id, @RequestBody @Valid DosageFormRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(DosageFormResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
