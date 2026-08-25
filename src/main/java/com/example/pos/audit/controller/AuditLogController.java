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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService service;
    public AuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('audit.read')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponseDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String recordId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        Page<AuditLog> page;
        if (fromDate != null || toDate != null) {
            java.time.LocalDateTime from = fromDate == null
                    ? java.time.LocalDateTime.of(2000, 1, 1, 0, 0) : fromDate.atStartOfDay();
            java.time.LocalDateTime to = toDate == null
                    ? java.time.LocalDate.now().plusDays(1).atStartOfDay()
                    : toDate.plusDays(1).atStartOfDay();
            page = service.getByDateRange(from, to, pageable);
        } else if (userId != null) page = service.getByUser(userId, pageable);
        else if (tableName != null) page = service.getByTable(tableName, recordId, pageable);
        else page = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, AuditLogResponseDto::from)));
    }
}
