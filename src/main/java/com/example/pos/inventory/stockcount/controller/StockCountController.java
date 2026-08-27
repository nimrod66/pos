package com.example.pos.inventory.stockcount.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.inventory.stockcount.dto.StockCountRequestDto;
import com.example.pos.inventory.stockcount.dto.StockCountResponseDto;
import com.example.pos.inventory.stockcount.service.StockCountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-counts")
@PreAuthorize("hasAuthority('stock_count.read')")
public class StockCountController {

    private final StockCountService service;

    public StockCountController(StockCountService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('stock_count.write')")
    public ResponseEntity<ApiResponse<StockCountResponseDto>> create(
            @RequestBody @Valid StockCountRequestDto dto) {
        return ResponseEntity.ok(ApiResponse.created(service.createCount(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StockCountResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StockCountResponseDto> p = service.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(p)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockCountResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PatchMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('stock_count.write')")
    public ResponseEntity<ApiResponse<StockCountResponseDto>> reconcile(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.reconcileCount(id)));
    }
}
