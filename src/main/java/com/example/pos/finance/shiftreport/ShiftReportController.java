package com.example.pos.finance.shiftreport;

import com.example.pos.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ShiftReportController {

    private final ShiftReportService reportService;

    public ShiftReportController(ShiftReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/shift-z/{id}")
    public ResponseEntity<ApiResponse<ShiftReport>> getZReport(@PathVariable Long id) {
        ShiftReport report = reportService.generate(id);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
