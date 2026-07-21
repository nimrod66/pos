package com.example.pos.procurement.suppliers.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.procurement.suppliers.dto.SupplierRequestDto;
import com.example.pos.procurement.suppliers.dto.SupplierResponseDto;
import com.example.pos.procurement.suppliers.model.Suppliers;
import com.example.pos.procurement.suppliers.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;
    public SupplierController(SupplierService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDto>> create(@RequestBody @Valid SupplierRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(SupplierResponseDto.from(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll().stream().map(SupplierResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierResponseDto.from(service.getById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> update(@PathVariable Long id, @RequestBody @Valid SupplierRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.updated(SupplierResponseDto.from(service.update(id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(ApiResponse.deleted()); }
}
