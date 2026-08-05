package com.example.pos.compliance.transmission.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.compliance.gateway.ComplianceGatewayFactory;
import com.example.pos.compliance.transmission.dto.TransmissionResponseDto;
import com.example.pos.compliance.transmission.model.Transmission;
import com.example.pos.compliance.transmission.service.TransmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/etims")
public class TransmissionController {

    private final TransmissionService transmissionService;
    private final ComplianceGatewayFactory gatewayFactory;

    public TransmissionController(TransmissionService transmissionService, ComplianceGatewayFactory gatewayFactory) {
        this.transmissionService = transmissionService;
        this.gatewayFactory = gatewayFactory;
    }

    @PostMapping("/transmit/{invoiceId}")
    public ResponseEntity<ApiResponse<TransmissionResponseDto>> transmit(
            @PathVariable UUID invoiceId,
            @RequestParam(required = false) UUID submittedBy) {
        Transmission tx = transmissionService.createAndQueue(invoiceId, "TAX_INVOICE", submittedBy);
        return ResponseEntity.ok(ApiResponse.ok(TransmissionResponseDto.from(tx)));
    }

    @GetMapping("/transmissions/{id}")
    public ResponseEntity<ApiResponse<TransmissionResponseDto>> getTransmission(@PathVariable UUID id) {
        Transmission tx = transmissionService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(TransmissionResponseDto.from(tx)));
    }

    @GetMapping("/transmissions/by-invoice/{invoiceId}")
    public ResponseEntity<ApiResponse<TransmissionResponseDto>> getByInvoice(@PathVariable UUID invoiceId) {
        Transmission tx = transmissionService.getByInvoiceId(invoiceId);
        if (tx == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        return ResponseEntity.ok(ApiResponse.ok(TransmissionResponseDto.from(tx)));
    }

    @PostMapping("/retry/{transmissionId}")
    public ResponseEntity<ApiResponse<TransmissionResponseDto>> retry(@PathVariable UUID transmissionId) {
        transmissionService.requeueFailed();
        Transmission tx = transmissionService.getById(transmissionId);
        return ResponseEntity.ok(ApiResponse.ok(TransmissionResponseDto.from(tx)));
    }

    @PostMapping("/retry-all")
    public ResponseEntity<ApiResponse<String>> retryAll() {
        transmissionService.requeueFailed();
        return ResponseEntity.ok(ApiResponse.ok("Failed transmissions requeued"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok(gatewayFactory.getGateway("OSCU").getHealth()));
    }
}
