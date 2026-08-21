package com.example.pos.security.auth;

import com.example.pos.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfResponse>> csrf(HttpServletRequest request) {
        CsrfToken token = authService.getCsrfToken(request);
        CsrfResponse cr = new CsrfResponse(
                token != null ? token.getToken() : null,
                token != null ? token.getHeaderName() : null
        );
        return ResponseEntity.ok(ApiResponse.ok(cr));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MeResponse>> login(
            @RequestBody @Valid LoginRequestDto dto,
            HttpServletRequest request,
            HttpServletResponse response) {
        MeResponse result = authService.login(dto.getEmail(), dto.getPassword(), request, response);
        return ResponseEntity.ok(ApiResponse.ok(result, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(userDetails.getUserId(), request)));
    }

    @PostMapping("/branch")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MeResponse>> switchBranch(
            @RequestBody @Valid BranchSwitchRequest dto,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.switchBranch(userDetails.getUserId(), dto.branchId(), request, response),
                "Active branch changed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.ok(new LogoutResponse(true), "Logged out"));
    }

    public record CsrfResponse(String token, String headerName) {}

    public record LogoutResponse(boolean signedOut) {}

    public record BranchSwitchRequest(
            @jakarta.validation.constraints.NotNull UUID branchId) {}
}
