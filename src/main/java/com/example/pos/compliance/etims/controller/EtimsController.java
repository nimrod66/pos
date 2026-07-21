package com.example.pos.compliance.etims.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.compliance.etims.dto.EtimsResponseDto;
import com.example.pos.compliance.etims.service.EtimsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etims")
public class EtimsController {

    private final EtimsService service;
    public EtimsController(EtimsService service) { this.service = service; }

    @PostMapping("/{saleId}")
    public ResponseEntity<ApiResponse<EtimsResponseDto>> submit(
            @PathVariable Long saleId, @RequestParam String qrCode, @RequestParam(defaultValue = "SUBMITTED") String status) {
        return ResponseEntity.ok(ApiResponse.created(EtimsResponseDto.from(service.submit(saleId, qrCode, status))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EtimsResponseDto>>> getBySale(@RequestParam Long saleId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getBySale(saleId).stream().map(EtimsResponseDto::from).toList()));
    }
}
