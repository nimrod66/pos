package com.example.pos.audit.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.audit.dto.AuditLogResponseDto;
import com.example.pos.audit.model.AuditLog;
import com.example.pos.audit.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService service;
    public AuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String recordId,
            @RequestParam(required = false) UUID userId) {
        Page<AuditLog> page;
        if (userId != null) page = service.getByUser(userId, pageable);
        else if (tableName != null) page = service.getByTable(tableName, recordId, pageable);
        else page = Page.empty();
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, AuditLogResponseDto::from)));
    }
}
