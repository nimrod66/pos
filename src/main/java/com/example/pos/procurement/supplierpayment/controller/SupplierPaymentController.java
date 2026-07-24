package com.example.pos.procurement.supplierpayment.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.supplierpayment.dto.SupplierPaymentRequestDto;
import com.example.pos.procurement.supplierpayment.dto.SupplierPaymentResponseDto;
import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import com.example.pos.procurement.supplierpayment.service.SupplierPaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier-payments")
public class SupplierPaymentController {

    private final SupplierPaymentService service;
    public SupplierPaymentController(SupplierPaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierPaymentResponseDto>> pay(@RequestBody @Valid SupplierPaymentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(SupplierPaymentResponseDto.from(service.makePayment(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SupplierPaymentResponseDto>>> getByInvoice(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam Long invoiceId) {
        Page<SupplierPayment> page = service.getByInvoice(invoiceId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, SupplierPaymentResponseDto::from)));
    }
}
