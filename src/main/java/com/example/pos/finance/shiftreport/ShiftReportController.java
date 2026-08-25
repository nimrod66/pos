package com.example.pos.finance.shiftreport;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ShiftReportController {

    private final ShiftReportService reportService;

    public ShiftReportController(ShiftReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/shift-z/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(
            "hasAnyAuthority('shift.close', 'shift.variance.approve', 'sale.read')")
    public ResponseEntity<ApiResponse<ShiftReport>> getZReport(@PathVariable UUID id) {
        ShiftReport report = reportService.generate(id);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
