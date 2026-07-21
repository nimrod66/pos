package com.example.pos.sale.receipts.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.sale.receipts.service.ReceiptData;
import com.example.pos.sale.receipts.service.ReceiptService;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final SalesRepository salesRepo;
    private final ReceiptService receiptService;

    public ReceiptController(SalesRepository salesRepo, ReceiptService receiptService) {
        this.salesRepo = salesRepo;
        this.receiptService = receiptService;
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<ApiResponse<ReceiptData>> getReceipt(@PathVariable Long saleId) {
        Sales sale = salesRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        return ResponseEntity.ok(ApiResponse.ok(receiptService.generate(sale)));
    }

    @GetMapping("/{saleId}/print")
    public ResponseEntity<String> getReceiptEscPos(@PathVariable Long saleId) {
        Sales sale = salesRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        return ResponseEntity.ok(receiptService.generateEscPos(sale));
    }
}
