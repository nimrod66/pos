package com.example.pos.reporting.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.reporting.dto.DashboardResponseDto;
import com.example.pos.reporting.dto.InventoryReportResponseDto;
import com.example.pos.reporting.service.BranchScopeService;
import com.example.pos.reporting.service.ReportingService;
import com.example.pos.security.auth.UserDetailsImpl;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService service;
    private final BranchScopeService branchScopeService;

    public ReportingController(ReportingService service, BranchScopeService branchScopeService) {
        this.service = service;
        this.branchScopeService = branchScopeService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('report.sales.read')")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboard(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID pharmacyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return ResponseEntity.ok(ApiResponse.ok(service.getDashboard(
                branchScopeService.resolve(principal, branchId, pharmacyId), start, end)));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('report.inventory.read')")
    public ResponseEntity<ApiResponse<InventoryReportResponseDto>> getInventoryReport(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID pharmacyId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getInventoryReport(
                branchScopeService.resolve(principal, branchId, pharmacyId))));
    }
}
