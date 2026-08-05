package com.example.pos.sale.sales.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.sale.sales.dto.SaleRequestDto;
import com.example.pos.sale.sales.dto.SaleResponseDto;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SaleService saleService;

    public SalesController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponseDto>> create(@RequestBody @Valid SaleRequestDto dto) {
        Sales sale = saleService.createSale(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(saleService.toResponseDto(sale)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SaleResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID branchId) {
        Page<Sales> page = saleService.getSalesByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, saleService::toResponseDto)));
    }

    @GetMapping("/suspended")
    public ResponseEntity<ApiResponse<List<SaleResponseDto>>> getSuspended(
            @RequestParam UUID branchId) {
        List<Sales> sales = saleService.getSuspendedSales(branchId);
        List<SaleResponseDto> response = sales.stream()
                .map(saleService::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/last")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getLast(
            @RequestParam UUID userId,
            @RequestParam UUID branchId) {
        Sales sale = saleService.getLastSaleByUserAndBranch(userId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(
                sale != null ? saleService.toResponseDto(sale) : null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getById(@PathVariable UUID id) {
        Sales sale = saleService.getSaleById(id);
        return ResponseEntity.ok(ApiResponse.ok(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SaleResponseDto>> cancel(@PathVariable UUID id) {
        Sales sale = saleService.cancelSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<SaleResponseDto>> suspend(@PathVariable UUID id) {
        Sales sale = saleService.suspendSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<SaleResponseDto>> resume(@PathVariable UUID id) {
        Sales sale = saleService.resumeSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/items/{itemId}/override-price")
    public ResponseEntity<ApiResponse<SaleResponseDto>> overridePrice(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestBody @Valid PriceOverrideDto dto) {
        Sales sale = saleService.overrideItemPrice(id, itemId, dto.getNewPrice(), dto.getReason());
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    public static class PriceOverrideDto {
        private BigDecimal newPrice;
        private String reason;

        public BigDecimal getNewPrice() { return newPrice; }
        public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
