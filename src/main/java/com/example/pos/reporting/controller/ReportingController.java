package com.example.pos.reporting.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.reporting.dto.DashboardReportDto;
import com.example.pos.reporting.dto.InventoryReportDto;
import com.example.pos.reporting.dto.SalesReportDto;
import com.example.pos.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService service;

    public ReportingController(ReportingService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard.read')")
    public ResponseEntity<ApiResponse<DashboardReportDto>> getDashboard(
            @RequestParam UUID branchId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "false") boolean pharmacyWide) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getDashboard(branchId, date, pharmacyWide)));
    }

    @GetMapping("/sales-summary")
    @PreAuthorize("hasAuthority('report.sales.read')")
    public ResponseEntity<ApiResponse<SalesReportDto>> getSalesReport(
            @RequestParam UUID branchId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "false") boolean pharmacyWide) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getSalesReport(branchId, from, to, pharmacyWide)));
    }

    @GetMapping("/inventory-summary")
    @PreAuthorize("hasAuthority('report.inventory.read')")
    public ResponseEntity<ApiResponse<InventoryReportDto>> getInventoryReport(
            @RequestParam UUID branchId,
            @RequestParam(required = false) LocalDate asOf,
            @RequestParam(defaultValue = "false") boolean pharmacyWide) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getInventoryReport(branchId, asOf, pharmacyWide)));
    }

    /** PLU report: per-medicine sold quantity and revenue vs remaining stock. */
    @GetMapping("/plu")
    @PreAuthorize("hasAuthority('report.sales.read')")
    public ResponseEntity<ApiResponse<List<com.example.pos.reporting.dto.PluRowDto>>> getPluReport(
            @RequestParam UUID branchId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPluReport(branchId, from, to)));
    }
}
