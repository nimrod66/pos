package com.example.pos.masterdata.tax.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.masterdata.tax.dto.TaxRequestDto;
import com.example.pos.masterdata.tax.dto.TaxResponseDto;
import com.example.pos.masterdata.tax.model.Tax;
import com.example.pos.masterdata.tax.service.TaxService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-categories")
public class TaxController {

    private final TaxService service;
    public TaxController(TaxService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<TaxResponseDto>> create(@RequestBody @Valid TaxRequestDto dto) {
        Tax tax = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(TaxResponseDto.from(tax)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TaxResponseDto>>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Tax> page = activeOnly ? service.getActive(pageable) : service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, TaxResponseDto::from)));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<TaxResponseDto>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(TaxResponseDto.from(service.getByCode(code))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(TaxResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxResponseDto>> update(@PathVariable UUID id, @RequestBody @Valid TaxRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(TaxResponseDto.from(service.update(id, dto))));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<TaxResponseDto>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.updated(TaxResponseDto.from(service.toggleActive(id))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
