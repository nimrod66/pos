package com.example.pos.compliance.monitoring.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.compliance.monitoring.ComplianceDashboardService;
import com.example.pos.compliance.monitoring.dto.ComplianceDashboardDto;
import com.example.pos.compliance.certification.CertificationService;
import com.example.pos.compliance.monitoring.health.ComplianceHealthIndicator;
import com.example.pos.compliance.sync.SyncEngine;
import com.example.pos.compliance.sync.SyncState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/compliance")
public class DashboardController {

    private final ComplianceDashboardService dashboardService;
    private final SyncEngine syncEngine;
    private final ComplianceHealthIndicator healthIndicator;
    private final CertificationService certificationService;

    public DashboardController(ComplianceDashboardService dashboardService,
                               SyncEngine syncEngine,
                               ComplianceHealthIndicator healthIndicator,
                               CertificationService certificationService) {
        this.dashboardService = dashboardService;
        this.syncEngine = syncEngine;
        this.healthIndicator = healthIndicator;
        this.certificationService = certificationService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ComplianceDashboardDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(healthIndicator.health()));
    }

    @GetMapping("/sync/status")
    public ResponseEntity<ApiResponse<List<SyncState>>> syncStatus() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSyncStatus()));
    }

    @PostMapping("/sync/run")
    public ResponseEntity<ApiResponse<String>> runSync(@RequestParam(defaultValue = "all") String scope) {
        switch (scope.toUpperCase()) {
            case "CODE": syncEngine.runCodeSync(); break;
            case "ITEM": syncEngine.runItemSync(); break;
            case "BRANCH": syncEngine.runBranchSync(); break;
            case "PURCHASE": syncEngine.runPurchaseSync(); break;
            case "STOCK": syncEngine.runStockSync(); break;
            case "INVOICE": syncEngine.runInvoiceSync(); break;
            default: syncEngine.runAll(); break;
        }
        return ResponseEntity.ok(ApiResponse.ok("Sync completed: " + scope));
    }

    @PostMapping("/certification/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runCertificationSuite() {
        return ResponseEntity.ok(ApiResponse.ok(certificationService.runCertificationSuite()));
    }

    @PostMapping("/certification/generate-demo-data")
    public ResponseEntity<ApiResponse<String>> generateDemoData() {
        certificationService.generateDemoData();
        return ResponseEntity.ok(ApiResponse.ok("Demo data generated"));
    }

    @PostMapping("/certification/export")
    public ResponseEntity<ApiResponse<String>> exportArtifacts() {
        return ResponseEntity.ok(ApiResponse.ok(certificationService.exportCertificationArtifacts()));
    }
}
