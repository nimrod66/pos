package com.example.pos.notification.controller;

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
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponseDto>>> getByBranch(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Page<Notification> page = unreadOnly
                ? service.getUnreadByBranch(branchId, pageable)
                : service.getByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(page, NotificationResponseDto::from)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(NotificationResponseDto.from(service.markRead(id))));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable Long id) {
        service.dismiss(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
