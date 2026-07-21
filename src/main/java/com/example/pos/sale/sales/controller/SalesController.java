package com.example.pos.sale.sales.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.sale.sales.dto.SaleRequestDto;
import com.example.pos.sale.sales.dto.SaleResponseDto;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
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
    public ResponseEntity<ApiResponse<List<SaleResponseDto>>> getAll(
            @RequestParam(required = false) Long branchId) {
        List<Sales> sales = saleService.getSalesByBranch(branchId);
        List<SaleResponseDto> response = sales.stream()
                .map(saleService::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/suspended")
    public ResponseEntity<ApiResponse<List<SaleResponseDto>>> getSuspended(
            @RequestParam Long branchId) {
        List<Sales> sales = saleService.getSuspendedSales(branchId);
        List<SaleResponseDto> response = sales.stream()
                .map(saleService::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/last")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getLast(
            @RequestParam Long userId,
            @RequestParam Long branchId) {
        Sales sale = saleService.getLastSaleByUserAndBranch(userId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(
                sale != null ? saleService.toResponseDto(sale) : null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getById(@PathVariable Long id) {
        Sales sale = saleService.getSaleById(id);
        return ResponseEntity.ok(ApiResponse.ok(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SaleResponseDto>> cancel(@PathVariable Long id) {
        Sales sale = saleService.cancelSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<SaleResponseDto>> suspend(@PathVariable Long id) {
        Sales sale = saleService.suspendSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<SaleResponseDto>> resume(@PathVariable Long id) {
        Sales sale = saleService.resumeSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/items/{itemId}/override-price")
    public ResponseEntity<ApiResponse<SaleResponseDto>> overridePrice(
            @PathVariable Long id,
            @PathVariable Long itemId,
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
