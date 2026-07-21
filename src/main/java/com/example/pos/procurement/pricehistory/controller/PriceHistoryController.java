package com.example.pos.procurement.pricehistory.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.procurement.pricehistory.dto.PriceHistoryResponseDto;
import com.example.pos.procurement.pricehistory.model.PriceHistory;
import com.example.pos.procurement.pricehistory.service.PriceHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-history")
public class PriceHistoryController {

    private final PriceHistoryService service;

    public PriceHistoryController(PriceHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PriceHistoryResponseDto>>> getByMedicine(
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) Long batchId) {
        List<PriceHistory> histories;
        if (medicineId != null) {
            histories = service.getByMedicine(medicineId);
        } else if (batchId != null) {
            histories = service.getByBatch(batchId);
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Provide medicineId or batchId"));
        }
        List<PriceHistoryResponseDto> response = histories.stream()
                .map(PriceHistoryResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
