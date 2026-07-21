package com.example.pos.security.auth;

import com.example.pos.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(
            @RequestBody @Valid LoginRequestDto dto,
            HttpServletRequest request, HttpServletResponse response) {
        LoginResult result = authService.login(dto.getEmail(), dto.getPassword(), request, response);
        return ResponseEntity.ok(ApiResponse.ok(result, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(
            HttpServletRequest request, HttpServletResponse response) {
        String newToken = authService.refresh(request, response);
        return ResponseEntity.ok(ApiResponse.ok(newToken, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out"));
    }
}
