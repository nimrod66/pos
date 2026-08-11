package com.example.pos.inventory.batches.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.inventory.batches.dto.MedicineBatchRequestDto;
import com.example.pos.inventory.batches.dto.MedicineBatchResponseDto;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.service.MedicineBatchesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/batches")
public class MedicineBatchesController {

    private final MedicineBatchesService batchService;

    public MedicineBatchesController(MedicineBatchesService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory.adjust.approve')")
    public ResponseEntity<ApiResponse<MedicineBatchResponseDto>> create(
            @RequestBody @Valid MedicineBatchRequestDto dto) {
        MedicineBatches batch = batchService.createBatch(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MedicineBatchResponseDto.from(batch)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<ApiResponse<PagedResponse<MedicineBatchResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) Boolean expiring,
            @RequestParam(required = false) LocalDate before) {
        Page<MedicineBatches> page;
        if (expiring != null && expiring) {
            page = batchService.getBatchesExpiringBefore(before != null ? before : LocalDate.now().plusDays(90), pageable);
        } else if (medicineId != null) {
            page = batchService.getBatchesByMedicine(medicineId, pageable);
        } else {
            page = batchService.getAllBatches(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, MedicineBatchResponseDto::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.read')")
    public ResponseEntity<ApiResponse<MedicineBatchResponseDto>> getById(@PathVariable UUID id) {
        MedicineBatches batch = batchService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.ok(MedicineBatchResponseDto.from(batch)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.adjust.approve')")
    public ResponseEntity<ApiResponse<MedicineBatchResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody @Valid MedicineBatchRequestDto dto) {
        MedicineBatches batch = batchService.updateBatch(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(MedicineBatchResponseDto.from(batch)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory.adjust.approve')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
