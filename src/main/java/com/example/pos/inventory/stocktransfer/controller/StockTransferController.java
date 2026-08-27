package com.example.pos.inventory.stocktransfer.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.inventory.stocktransfer.dto.StockTransferRequestDto;
import com.example.pos.inventory.stocktransfer.dto.StockTransferResponseDto;
import com.example.pos.inventory.stocktransfer.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-transfers")
@PreAuthorize("hasAuthority('stock_transfer.read')")
public class StockTransferController {

    private final StockTransferService service;

    public StockTransferController(StockTransferService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('stock_transfer.write')")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> create(
            @RequestBody @Valid StockTransferRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.created(service.createTransfer(dto)));
    }

    @GetMapping("/out")
    public ResponseEntity<ApiResponse<PagedResponse<StockTransferResponseDto>>> getTransfersOut(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StockTransferResponseDto> p = service.getTransfersOut(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(p)));
    }

    @GetMapping("/in")
    public ResponseEntity<ApiResponse<PagedResponse<StockTransferResponseDto>>> getTransfersIn(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StockTransferResponseDto> p = service.getTransfersIn(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(p)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('stock_transfer.write')")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.approveTransfer(id)));
    }

    @PatchMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('stock_transfer.write')")
    public ResponseEntity<ApiResponse<StockTransferResponseDto>> receive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.receiveTransfer(id)));
    }
}
