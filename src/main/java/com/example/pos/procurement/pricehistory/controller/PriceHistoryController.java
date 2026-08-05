package com.example.pos.procurement.pricehistory.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.procurement.pricehistory.dto.PriceHistoryResponseDto;
import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.pricehistory.service.PriceHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/price-history")
public class PriceHistoryController {

    private final PriceHistoryService service;

    public PriceHistoryController(PriceHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PriceHistoryResponseDto>>> getByMedicine(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) UUID batchId) {
        Page<PriceHistory> page;
        if (medicineId != null) {
            page = service.getByMedicine(medicineId, pageable);
        } else if (batchId != null) {
            page = service.getByBatch(batchId, pageable);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Provide medicineId or batchId"));
        }
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, PriceHistoryResponseDto::from)));
    }
}
