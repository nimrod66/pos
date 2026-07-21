package com.example.pos.procurement.supplierpayment.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.procurement.supplierpayment.dto.SupplierPaymentRequestDto;
import com.example.pos.procurement.supplierpayment.dto.SupplierPaymentResponseDto;
import com.example.pos.procurement.supplierpayment.model.SupplierPayment;
import com.example.pos.procurement.supplierpayment.service.SupplierPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<List<SupplierPaymentResponseDto>>> getByInvoice(@RequestParam Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getByInvoice(invoiceId).stream().map(SupplierPaymentResponseDto::from).toList()));
    }
}
