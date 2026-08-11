package com.example.pos.procurement.suppliers.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.suppliers.dto.SupplierRequestDto;
import com.example.pos.procurement.suppliers.dto.SupplierResponseDto;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService service;
    public SupplierController(SupplierService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('supplier.write')")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> create(@RequestBody @Valid SupplierRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(SupplierResponseDto.from(service.create(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('supplier.read')")
    public ResponseEntity<ApiResponse<PagedResponse<SupplierResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Suppliers> page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, SupplierResponseDto::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier.read')")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponseDto.from(service.getById(id))));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('supplier.read')")
    public ResponseEntity<ApiResponse<PagedResponse<SupplierResponseDto>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Suppliers> page = service.search(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, SupplierResponseDto::from)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier.write')")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> update(@PathVariable UUID id, @RequestBody @Valid SupplierRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(SupplierResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.ok(ApiResponse.deleted()); }
}
