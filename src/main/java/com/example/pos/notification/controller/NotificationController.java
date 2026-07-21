package com.example.pos.notification.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.notification.dto.NotificationResponseDto;
import com.example.pos.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getByBranch(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        List<NotificationResponseDto> list = (unreadOnly
                ? service.getUnreadByBranch(branchId)
                : service.getByBranch(branchId))
                .stream().map(NotificationResponseDto::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
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
