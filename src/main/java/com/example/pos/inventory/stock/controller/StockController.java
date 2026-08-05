package com.example.pos.inventory.stock.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.inventory.stock.dto.StockAdjustmentDto;
import com.example.pos.inventory.stock.dto.StockRequestDto;
import com.example.pos.inventory.stock.dto.StockResponseDto;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.service.StockService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockResponseDto>> create(
            @RequestBody @Valid StockRequestDto dto) {
        Stock stock = stockService.createStock(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(StockResponseDto.from(stock)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StockResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID branchId) {
        Page<Stock> page = stockService.getStockByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, StockResponseDto::from)));
    }

    @GetMapping("/low")
    public ResponseEntity<ApiResponse<List<StockResponseDto>>> getLowStock(
            @RequestParam UUID branchId) {
        List<Stock> stockList = stockService.getLowStockByBranch(branchId);
        List<StockResponseDto> response = stockList.stream()
                .map(StockResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockResponseDto>> getById(@PathVariable UUID id) {
        Stock stock = stockService.getStockById(id);
        return ResponseEntity.ok(ApiResponse.ok(StockResponseDto.from(stock)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid StockRequestDto dto) {
        Stock stock = stockService.updateStock(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(StockResponseDto.from(stock)));
    }

    @PostMapping("/receive")
    public ResponseEntity<ApiResponse<StockResponseDto>> receive(
            @RequestBody @Valid StockAdjustmentDto dto) {
        Stock stock = stockService.receiveStock(dto);
        return ResponseEntity.ok(ApiResponse.updated(StockResponseDto.from(stock)));
    }

    @PostMapping("/deduct")
    public ResponseEntity<ApiResponse<StockResponseDto>> deduct(
            @RequestBody @Valid StockAdjustmentDto dto) {
        Stock stock = stockService.deductStock(dto);
        return ResponseEntity.ok(ApiResponse.updated(StockResponseDto.from(stock)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        stockService.deleteStock(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
