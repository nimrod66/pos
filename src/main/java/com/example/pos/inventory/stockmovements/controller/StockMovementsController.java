package com.example.pos.inventory.stockmovements.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.inventory.stockmovements.dto.StockMovementRequestDto;
import com.example.pos.inventory.stockmovements.dto.StockMovementResponseDto;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.service.StockMovementsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stock-movements")
public class StockMovementsController {

    private final StockMovementsService movementsService;

    public StockMovementsController(StockMovementsService movementsService) {
        this.movementsService = movementsService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('inventory.adjust.request', 'inventory.adjust.approve')")
    public ResponseEntity<ApiResponse<StockMovementResponseDto>> record(
            @RequestBody @Valid StockMovementRequestDto dto) {
        StockMovements movement = movementsService.recordMovement(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(StockMovementResponseDto.from(movement)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<ApiResponse<PagedResponse<StockMovementResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        Page<StockMovements> page;
        if (branchId != null && start != null && end != null) {
            page = movementsService.getMovementsByBranchAndDateRange(branchId, start, end, pageable);
        } else if (batchId != null) {
            page = movementsService.getMovementsByBatch(batchId, pageable);
        } else {
            page = movementsService.getMovementsByBranch(branchId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, StockMovementResponseDto::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<ApiResponse<StockMovementResponseDto>> getById(@PathVariable UUID id) {
        StockMovements movement = movementsService.getMovementById(id);
        return ResponseEntity.ok(ApiResponse.ok(StockMovementResponseDto.from(movement)));
    }
}
