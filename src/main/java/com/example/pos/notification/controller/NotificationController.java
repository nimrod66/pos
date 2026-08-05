package com.example.pos.notification.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.common.dto.PagedResponse;
import com.example.pos.notification.dto.NotificationResponseDto;
import com.example.pos.notification.model.Notification;
import com.example.pos.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponseDto>>> getByBranch(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Page<Notification> page = unreadOnly
                ? service.getUnreadByBranch(branchId, pageable)
                : service.getByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, NotificationResponseDto::from)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(NotificationResponseDto.from(service.markRead(id))));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable UUID id) {
        service.dismiss(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
