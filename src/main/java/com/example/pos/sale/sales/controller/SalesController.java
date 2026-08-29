package com.example.pos.sale.sales.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.common.exception.BaseException;
import com.example.pos.operations.model.OperationalMetricEvent;
import com.example.pos.operations.service.OperationalMetricsService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SaleService saleService;
    private final OperationalMetricsService metricsService;

    public SalesController(SaleService saleService, OperationalMetricsService metricsService) {
        this.saleService = saleService;
        this.metricsService = metricsService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pos.sell')")
    public ResponseEntity<ApiResponse<SaleResponseDto>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid SaleRequestDto dto) {
        long started = System.nanoTime();
        metricsService.record(OperationalMetricEvent.EventType.CHECKOUT,
                OperationalMetricEvent.EventStatus.ATTEMPTED, null, "sales-api", null,
                null, idempotencyKey, null, null);
        try {
            Sales sale = saleService.createSale(dto, idempotencyKey);
            metricsService.record(OperationalMetricEvent.EventType.CHECKOUT,
                    OperationalMetricEvent.EventStatus.SUCCESS, null, "sales-api", sale.getTerminalId(),
                    sale.getId(), idempotencyKey, elapsedMs(started), null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(saleService.toResponseDto(sale)));
        } catch (RuntimeException ex) {
            metricsService.record(OperationalMetricEvent.EventType.CHECKOUT,
                    OperationalMetricEvent.EventStatus.FAILED, reasonCode(ex), "sales-api", null,
                    null, idempotencyKey, elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private String reasonCode(RuntimeException ex) {
        if (ex instanceof BaseException base && base.getErrorCode() != null) {
            return base.getErrorCode();
        }
        return ex.getClass().getSimpleName();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sale.read')")
    public ResponseEntity<ApiResponse<PagedResponse<SaleResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID branchId) {
        Page<Sales> page = saleService.getSalesByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, saleService::toResponseDto)));
    }

    @GetMapping("/suspended")
    @PreAuthorize("hasAuthority('sale.read')")
    public ResponseEntity<ApiResponse<List<SaleResponseDto>>> getSuspended(
            @RequestParam(required = false) UUID branchId) {
        List<Sales> sales = saleService.getSuspendedSales(branchId);
        List<SaleResponseDto> response = sales.stream()
                .map(saleService::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/last")
    @PreAuthorize("hasAuthority('sale.read')")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getLast(
            @RequestParam UUID userId,
            @RequestParam UUID branchId) {
        Sales sale = saleService.getLastSaleByUserAndBranch(userId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(
                sale != null ? saleService.toResponseDto(sale) : null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sale.read')")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getById(@PathVariable UUID id) {
        Sales sale = saleService.getSaleById(id);
        return ResponseEntity.ok(ApiResponse.ok(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sale.void')")
    public ResponseEntity<ApiResponse<SaleResponseDto>> cancel(@PathVariable UUID id) {
        Sales sale = saleService.cancelSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('pos.sell')")
    public ResponseEntity<ApiResponse<SaleResponseDto>> suspend(@PathVariable UUID id) {
        Sales sale = saleService.suspendSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('pos.sell')")
    public ResponseEntity<ApiResponse<SaleResponseDto>> resume(@PathVariable UUID id) {
        Sales sale = saleService.resumeSale(id);
        return ResponseEntity.ok(ApiResponse.updated(saleService.toResponseDto(sale)));
    }

    @PatchMapping("/{id}/items/{itemId}/override-price")
    @PreAuthorize("hasAuthority('medicine.price.write')")
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
