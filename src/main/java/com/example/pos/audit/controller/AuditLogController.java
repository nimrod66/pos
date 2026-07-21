package com.example.pos.audit.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.audit.dto.AuditLogResponseDto;
import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService service;
    public AuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getAll(
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String recordId,
            @RequestParam(required = false) Long userId) {
        List<AuditLog> logs;
        if (userId != null) logs = service.getByUser(userId);
        else if (tableName != null) logs = service.getByTable(tableName, recordId);
        else logs = List.of();
        return ResponseEntity.ok(ApiResponse.ok(logs.stream().map(AuditLogResponseDto::from).toList()));
    }
}
