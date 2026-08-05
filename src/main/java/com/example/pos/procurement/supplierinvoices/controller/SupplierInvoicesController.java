package com.example.pos.procurement.supplierinvoices.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.supplierinvoices.dto.SupplierInvoiceRequestDto;
import com.example.pos.procurement.supplierinvoices.dto.SupplierInvoiceResponseDto;
import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import com.example.pos.procurement.supplierinvoices.service.SupplierInvoicesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/supplier-invoices")
public class SupplierInvoicesController {

    private final SupplierInvoicesService service;
    public SupplierInvoicesController(SupplierInvoicesService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierInvoiceResponseDto>> create(@RequestBody @Valid SupplierInvoiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(SupplierInvoiceResponseDto.from(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SupplierInvoiceResponseDto>>> getBySupplier(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam UUID supplierId) {
        Page<SupplierInvoices> page = service.getBySupplier(supplierId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, SupplierInvoiceResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierInvoiceResponseDto.from(service.getById(id))));
    }
}
