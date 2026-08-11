package com.example.pos.procurement.purchaseorders.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderRequestDto;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderResponseDto;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.service.PurchaseOrdersService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrdersController {

    private final PurchaseOrdersService service;
    public PurchaseOrdersController(PurchaseOrdersService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('supplier.write')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> create(@RequestBody @Valid PurchaseOrderRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.toDto(service.create(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('supplier.read')")
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseOrderResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID branchId, @RequestParam(required = false) UUID supplierId) {
        Page<PurchaseOrders> page = supplierId != null
                ? service.getBySupplier(supplierId, pageable)
                : service.getByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, service::toDto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier.read')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.toDto(service.getById(id))));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory.adjust.approve')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> approve(@PathVariable UUID id, @RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.updated(service.toDto(service.approve(id, userId))));
    }

    @PatchMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('inventory.receive')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> deliver(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.updated(service.toDto(service.markDelivered(id))));
    }
}
