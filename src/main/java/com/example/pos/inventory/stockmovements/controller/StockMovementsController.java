package com.example.pos.inventory.stockmovements.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.inventory.stockmovements.dto.StockMovementRequestDto;
import com.example.pos.inventory.stockmovements.dto.StockMovementResponseDto;
import com.example.pos.inventory.stockmovements.model.StockMovements;
import com.example.pos.inventory.stockmovements.service.StockMovementsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementsController {

    private final StockMovementsService movementsService;

    public StockMovementsController(StockMovementsService movementsService) {
        this.movementsService = movementsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockMovementResponseDto>> record(
            @RequestBody @Valid StockMovementRequestDto dto) {
        StockMovements movement = movementsService.recordMovement(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(StockMovementResponseDto.from(movement)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockMovementResponseDto>>> getAll(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<StockMovements> movements;
        if (branchId != null && start != null && end != null) {
            movements = movementsService.getMovementsByBranchAndDateRange(branchId, start, end);
        } else if (batchId != null) {
            movements = movementsService.getMovementsByBatch(batchId);
        } else if (branchId != null) {
            movements = movementsService.getMovementsByBranch(branchId);
        } else {
            movements = List.of();
        }
        List<StockMovementResponseDto> response = movements.stream()
                .map(StockMovementResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockMovementResponseDto>> getById(@PathVariable Long id) {
        StockMovements movement = movementsService.getMovementById(id);
        return ResponseEntity.ok(ApiResponse.ok(StockMovementResponseDto.from(movement)));
    }
}
