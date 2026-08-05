package com.example.pos.prescriptions.dispensary.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.prescriptions.dispensary.dto.DispensaryRequestDto;
import com.example.pos.prescriptions.dispensary.dto.DispensaryResponseDto;
import com.example.pos.prescriptions.dispensary.model.Dispensary;
import com.example.pos.prescriptions.dispensary.service.DispensaryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispensary")
public class DispensaryController {

    private final DispensaryService service;
    public DispensaryController(DispensaryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<DispensaryResponseDto>> dispense(@RequestBody @Valid DispensaryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(DispensaryResponseDto.from(service.dispense(dto))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DispensaryResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) UUID userId) {
        Page<Dispensary> page;
        if (batchId != null) page = service.getByBatch(batchId, pageable);
        else if (userId != null) page = service.getByUser(userId, pageable);
        else page = Page.empty();
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, DispensaryResponseDto::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DispensaryResponseDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(DispensaryResponseDto.from(service.getById(id))));
    }
}
