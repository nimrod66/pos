package com.example.pos.inventory.batches.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.inventory.batches.dto.MedicineBatchRequestDto;
import com.example.pos.inventory.batches.dto.MedicineBatchResponseDto;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.service.MedicineBatchesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class MedicineBatchesController {

    private final MedicineBatchesService batchService;

    public MedicineBatchesController(MedicineBatchesService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicineBatchResponseDto>> create(
            @RequestBody @Valid MedicineBatchRequestDto dto) {
        MedicineBatches batch = batchService.createBatch(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MedicineBatchResponseDto.from(batch)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicineBatchResponseDto>>> getAll(
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) Boolean expiring,
            @RequestParam(required = false) LocalDate before) {
        List<MedicineBatches> batches;
        if (expiring != null && expiring) {
            batches = batchService.getBatchesExpiringBefore(before != null ? before : LocalDate.now().plusDays(90));
        } else if (medicineId != null) {
            batches = batchService.getBatchesByMedicine(medicineId);
        } else {
            batches = batchService.getAllBatches();
        }
        List<MedicineBatchResponseDto> response = batches.stream()
                .map(MedicineBatchResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineBatchResponseDto>> getById(@PathVariable Long id) {
        MedicineBatches batch = batchService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.ok(MedicineBatchResponseDto.from(batch)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineBatchResponseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid MedicineBatchRequestDto dto) {
        MedicineBatches batch = batchService.updateBatch(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(MedicineBatchResponseDto.from(batch)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
