package com.example.pos.sync.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.sync.service.ConnectivityService;
import com.example.pos.sync.service.DeadLetterAlertService;
import com.example.pos.sync.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;
    private final ConnectivityService connectivityService;
    private final DeadLetterAlertService deadLetterAlertService;

    public SyncController(SyncService syncService, ConnectivityService connectivityService,
                          DeadLetterAlertService deadLetterAlertService) {
        this.syncService = syncService;
        this.connectivityService = connectivityService;
        this.deadLetterAlertService = deadLetterAlertService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "ok", "timestamp", java.time.LocalDateTime.now())));
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queueStatus() {
        return ResponseEntity.ok(ApiResponse.ok(syncService.getQueueStatus()));
    }

    @GetMapping("/connectivity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectivity() {
        return ResponseEntity.ok(ApiResponse.ok(connectivityService.getStatus()));
    }

    @GetMapping("/pull/catalog")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pullCatalog(
            @RequestParam(required = false) String since) {
        return ResponseEntity.ok(ApiResponse.ok(syncService.pullCatalog(since)));
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<Map<String, Object>>> push(@RequestBody Map<String, Object> event) {
        Map<String, Object> result = syncService.receivePush(event);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/dead-letter")
    public ResponseEntity<ApiResponse<Object>> listDeadLetters() {
        return ResponseEntity.ok(ApiResponse.ok(syncService.getDeadEvents()));
    }

    @GetMapping("/dead-letter/stats")
    public ResponseEntity<ApiResponse<Object>> deadLetterStats() {
        return ResponseEntity.ok(ApiResponse.ok(deadLetterAlertService.getDeadLetterStats()));
    }

    @PostMapping("/dead-letter/retry-all")
    public ResponseEntity<ApiResponse<Object>> retryAllDeadEvents() {
        return ResponseEntity.ok(ApiResponse.ok(syncService.retryAllDeadEvents()));
    }

    @PostMapping("/dead-letter/{eventId}/retry")
    public ResponseEntity<ApiResponse<Object>> retryDeadEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(ApiResponse.ok(syncService.retryDeadEvent(eventId)));
    }

    @DeleteMapping("/dead-letter/{eventId}")
    public ResponseEntity<ApiResponse<Object>> discardDeadEvent(@PathVariable String eventId) {
        syncService.discardDeadEvent(eventId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "DISCARDED", "eventId", eventId)));
    }

    @GetMapping("/dead-letter/{eventId}/export")
    public ResponseEntity<ApiResponse<Object>> exportDeadEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(ApiResponse.ok(syncService.exportDeadEvent(eventId)));
    }

    @GetMapping("/dead-letter/export-all")
    public ResponseEntity<ApiResponse<Object>> exportAllDeadEvents() {
        return ResponseEntity.ok(ApiResponse.ok(syncService.exportAllDeadEvents()));
    }

    @PostMapping("/dead-letter/terminal/{terminalId}/retry")
    public ResponseEntity<ApiResponse<Object>> retryTerminalDeadEvents(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(syncService.retryDeadEventsForTerminal(terminalId)));
    }
}
