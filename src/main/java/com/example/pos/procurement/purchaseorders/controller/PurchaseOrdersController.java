package com.example.pos.procurement.purchaseorders.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderRequestDto;
import com.example.pos.procurement.purchaseorders.dto.PurchaseOrderResponseDto;
import com.example.pos.procurement.purchaseorders.model.PurchaseOrders;
import com.example.pos.procurement.purchaseorders.service.PurchaseOrdersService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrdersController {

    private final PurchaseOrdersService service;
    public PurchaseOrdersController(PurchaseOrdersService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> create(@RequestBody @Valid PurchaseOrderRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.toDto(service.create(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponseDto>>> getAll(
            @RequestParam(required = false) Long branchId, @RequestParam(required = false) Long supplierId) {
        List<PurchaseOrders> list = branchId != null ? service.getByBranch(branchId)
                : supplierId != null ? service.getBySupplier(supplierId) : List.of();
        return ResponseEntity.ok(ApiResponse.ok(list.stream().map(service::toDto).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.toDto(service.getById(id))));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> approve(@PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.updated(service.toDto(service.approve(id, userId))));
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> deliver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.updated(service.toDto(service.markDelivered(id))));
    }
}
