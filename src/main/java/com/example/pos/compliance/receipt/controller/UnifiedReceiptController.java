package com.example.pos.compliance.receipt.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.compliance.receipt.dto.ReceiptDTO;
import com.example.pos.compliance.receipt.service.ReceiptAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts/fiscal")
@RequiredArgsConstructor
public class UnifiedReceiptController {

    private final ReceiptAssembler receiptAssembler;

    @GetMapping("/{saleId}")
    public ResponseEntity<ApiResponse<ReceiptDTO>> getReceipt(@PathVariable Long saleId) {
        ReceiptDTO dto = receiptAssembler.assemble(saleId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}
