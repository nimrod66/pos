package com.example.pos.procurement.supplierinvoices.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.procurement.supplierinvoices.dto.SupplierInvoiceRequestDto;
import com.example.pos.procurement.supplierinvoices.dto.SupplierInvoiceResponseDto;
import com.example.pos.procurement.supplierinvoices.model.SupplierInvoices;
import com.example.pos.procurement.supplierinvoices.service.SupplierInvoicesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier-invoices")
public class SupplierInvoicesController {

    private final SupplierInvoicesService service;
    public SupplierInvoicesController(SupplierInvoicesService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierInvoiceResponseDto>> create(@RequestBody @Valid SupplierInvoiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(SupplierInvoiceResponseDto.from(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierInvoiceResponseDto>>> getBySupplier(@RequestParam Long supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getBySupplier(supplierId).stream().map(SupplierInvoiceResponseDto::from).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(SupplierInvoiceResponseDto.from(service.getById(id))));
    }
}
