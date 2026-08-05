package com.example.pos.masterdata.manufacturer.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.masterdata.manufacturer.dto.ManufacturerRequestDto;
import com.example.pos.masterdata.manufacturer.dto.ManufacturerResponseDto;
import com.example.pos.masterdata.manufacturer.model.Manufacturer;
import com.example.pos.masterdata.manufacturer.service.ManufacturerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manufacturers")
public class ManufacturerController {

    private final ManufacturerService service;
    public ManufacturerController(ManufacturerService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ManufacturerResponseDto>> create(@RequestBody @Valid ManufacturerRequestDto dto) {
        Manufacturer m = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(ManufacturerResponseDto.from(m)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ManufacturerResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Manufacturer> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, ManufacturerResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManufacturerResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ManufacturerResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ManufacturerResponseDto>> update(@PathVariable UUID id, @RequestBody @Valid ManufacturerRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(ManufacturerResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
