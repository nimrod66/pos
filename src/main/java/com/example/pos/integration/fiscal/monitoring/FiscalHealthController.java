package com.example.pos.integration.fiscal.monitoring;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.integration.fiscal.dto.v1.FiscalHealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class FiscalHealthController {

    private final FiscalHealthService fiscalHealthService;

    public FiscalHealthController(FiscalHealthService fiscalHealthService) {
        this.fiscalHealthService = fiscalHealthService;
    }

    @GetMapping("/fiscal")
    public ResponseEntity<ApiResponse<FiscalHealthResponse>> fiscalHealth() {
        return ResponseEntity.ok(ApiResponse.ok(fiscalHealthService.check()));
    }
}
