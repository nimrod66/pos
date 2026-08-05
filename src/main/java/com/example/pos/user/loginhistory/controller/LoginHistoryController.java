package com.example.pos.user.loginhistory.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.user.loginhistory.dto.LoginHistoryResponseDto;
import com.example.pos.user.loginhistory.model.LoginHistory;
import com.example.pos.user.loginhistory.service.LoginHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/login-history")
public class LoginHistoryController {

    private final LoginHistoryService service;

    public LoginHistoryController(LoginHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LoginHistoryResponseDto>>> getByUser(
            @RequestParam UUID userId) {
        List<LoginHistory> history = service.getByUser(userId);
        List<LoginHistoryResponseDto> response = history.stream()
                .map(LoginHistoryResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
