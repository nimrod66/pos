package com.example.pos.reporting.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService service;

    public ReportingController(ReportingService service) { this.service = service; }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('report.sales.read', 'report.inventory.read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDashboard(branchId)));
    }
}
