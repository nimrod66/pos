package com.example.pos.procurement.goodsreceived.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedRequestDto;
import com.example.pos.procurement.goodsreceived.dto.GoodsReceivedResponseDto;
import com.example.pos.procurement.goodsreceived.service.GoodsReceivedNotesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goods-received")
public class GoodsReceivedController {

    private final GoodsReceivedNotesService service;
    public GoodsReceivedController(GoodsReceivedNotesService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory.receive')")
    public ResponseEntity<ApiResponse<GoodsReceivedResponseDto>> receive(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid GoodsReceivedRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(GoodsReceivedResponseDto.from(service.receive(dto, idempotencyKey))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<ApiResponse<PagedResponse<GoodsReceivedResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID poId) {
        Page<GoodsReceivedResponseDto> page = poId == null
                ? service.getAll(pageable)
                : service.getByPurchaseOrder(poId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.fromPage(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<ApiResponse<GoodsReceivedResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }
}
