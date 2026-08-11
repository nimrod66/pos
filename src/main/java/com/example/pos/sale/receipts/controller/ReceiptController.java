package com.example.pos.sale.receipts.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.sale.receipts.service.ReceiptData;
import com.example.pos.sale.receipts.service.ReceiptService;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final SalesRepository salesRepo;
    private final ReceiptService receiptService;
    private final AuthenticatedUserContext current;

    public ReceiptController(SalesRepository salesRepo, ReceiptService receiptService,
                             AuthenticatedUserContext current) {
        this.salesRepo = salesRepo;
        this.receiptService = receiptService;
        this.current = current;
    }

    @GetMapping("/{saleId}")
    @PreAuthorize("hasAnyAuthority('sale.read', 'sale.receipt.reprint')")
    public ResponseEntity<ApiResponse<ReceiptData>> getReceipt(@PathVariable UUID saleId) {
        Sales sale = salesRepo.findDetailedByIdAndBranchId(saleId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        return ResponseEntity.ok(ApiResponse.ok(receiptService.generate(sale)));
    }

    @GetMapping("/{saleId}/print")
    @PreAuthorize("hasAuthority('sale.receipt.reprint')")
    public ResponseEntity<String> getReceiptEscPos(@PathVariable UUID saleId) {
        Sales sale = salesRepo.findDetailedByIdAndBranchId(saleId, current.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        return ResponseEntity.ok(receiptService.generateEscPos(sale));
    }
}
