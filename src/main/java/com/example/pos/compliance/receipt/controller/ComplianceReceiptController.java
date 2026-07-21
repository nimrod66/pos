package com.example.pos.compliance.receipt.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.compliance.receipt.model.Receipt;
import com.example.pos.compliance.receipt.service.ComplianceReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compliance/receipts")
public class ComplianceReceiptController {

    private final ComplianceReceiptService receiptService;

    public ComplianceReceiptController(ComplianceReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Receipt>> create(
            @RequestParam Long saleId,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam String receiptData,
            @RequestParam String businessName,
            @RequestParam(required = false) String kraPin,
            @RequestParam String branchCode,
            @RequestParam(required = false) String qrCodeContent,
            @RequestParam(required = false) String verificationUrl) {
        Receipt receipt = receiptService.create(saleId, invoiceId, receiptData,
                businessName, kraPin, branchCode, qrCodeContent, verificationUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(receipt));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Receipt>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(receiptService.getById(id)));
    }

    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<ApiResponse<Receipt>> getBySaleId(@PathVariable Long saleId) {
        return ResponseEntity.ok(ApiResponse.ok(receiptService.getBySaleId(saleId)));
    }

    @PostMapping("/{id}/reprint")
    public ResponseEntity<ApiResponse<Receipt>> reprint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(receiptService.reprint(id)));
    }
}
