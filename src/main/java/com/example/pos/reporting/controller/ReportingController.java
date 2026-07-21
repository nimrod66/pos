package com.example.pos.reporting.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService service;

    public ReportingController(ReportingService service) { this.service = service; }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @RequestParam Long branchId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDashboard(branchId)));
    }
}
