package com.example.pos.operations.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.operations.model.OperationalMetricEvent;
import com.example.pos.operations.service.OperationalMetricsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations/metrics")
public class OperationalMetricsController {

    private final OperationalMetricsService service;

    public OperationalMetricsController(OperationalMetricsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('dashboard.read', 'audit.read', 'settings.manage')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(7) : from;
        return ResponseEntity.ok(ApiResponse.ok(service.summary(start.atStartOfDay(), end.plusDays(1).atStartOfDay(), branchId)));
    }

    @PostMapping("/client-event")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> clientEvent(@RequestBody @Valid ClientMetricEventRequest request) {
        service.record(request.getEventType(), request.getStatus(), request.getReasonCode(),
                request.getSource(), request.getTerminalId(), request.getResourceId(),
                request.getIdempotencyKey(), request.getLatencyMs(), request.getDetails());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Data
    public static class ClientMetricEventRequest {
        @NotNull
        private OperationalMetricEvent.EventType eventType;
        @NotNull
        private OperationalMetricEvent.EventStatus status;
        private String reasonCode;
        private String source;
        private String terminalId;
        private UUID resourceId;
        private String idempotencyKey;
        private Long latencyMs;
        private String details;
    }
}
