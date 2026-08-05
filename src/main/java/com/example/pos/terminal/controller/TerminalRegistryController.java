package com.example.pos.terminal.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.terminal.dto.*;
import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.service.TerminalRegistrationService;
import com.example.pos.terminal.service.TerminalSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/terminals")
@RequiredArgsConstructor
public class TerminalRegistryController {

    private final TerminalRegistrationService registrationService;
    private final TerminalSessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> register(@Valid @RequestBody TerminalRegisterRequestDto request) {
        TerminalResponseDto terminal = registrationService.registerTerminal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(terminal));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TerminalResponseDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.listAll()));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<TerminalResponseDto>>> listPending() {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.listByStatus(TerminalStatus.PENDING)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TerminalResponseDto>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.listByStatus(TerminalStatus.ACTIVE)));
    }

    @GetMapping("/{terminalId}")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> getByTerminalId(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.getByTerminalId(terminalId)));
    }

    @PutMapping("/{terminalId}")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> updateMetadata(
            @PathVariable String terminalId, @Valid @RequestBody TerminalRegisterRequestDto request) {
        return ResponseEntity.ok(ApiResponse.updated(registrationService.updateMetadata(terminalId, request)));
    }

    @PostMapping("/{terminalId}/approve")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> approve(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.approve(terminalId), "Terminal approved"));
    }

    @PostMapping("/{terminalId}/deactivate")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> deactivate(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.deactivate(terminalId), "Terminal deactivated"));
    }

    @PostMapping("/{terminalId}/block")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> block(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.block(terminalId), "Terminal blocked"));
    }

    @PostMapping("/{terminalId}/regenerate-key")
    public ResponseEntity<ApiResponse<TerminalResponseDto>> regenerateKey(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.regenerateKey(terminalId), "API key regenerated"));
    }

    @GetMapping("/{terminalId}/peripherals")
    public ResponseEntity<ApiResponse<List<HardwarePeripheralDto>>> getPeripherals(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.getPeripherals(terminalId)));
    }

    @PostMapping("/{terminalId}/peripherals")
    public ResponseEntity<ApiResponse<HardwarePeripheralDto>> addPeripheral(
            @PathVariable String terminalId, @Valid @RequestBody HardwarePeripheralRequestDto request) {
        return ResponseEntity.ok(ApiResponse.created(registrationService.addPeripheral(terminalId, request)));
    }

    @PutMapping("/{terminalId}/peripherals")
    public ResponseEntity<ApiResponse<List<HardwarePeripheralDto>>> replacePeripherals(
            @PathVariable String terminalId, @Valid @RequestBody List<HardwarePeripheralRequestDto> requests) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.replacePeripherals(terminalId, requests)));
    }

    @DeleteMapping("/peripherals/{peripheralId}")
    public ResponseEntity<ApiResponse<Void>> removePeripheral(@PathVariable UUID peripheralId) {
        registrationService.removePeripheral(peripheralId);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @GetMapping("/{terminalId}/sessions")
    public ResponseEntity<ApiResponse<List<TerminalSessionResponseDto>>> getSessions(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getActiveSessions(
                registrationService.getTerminalEntity(terminalId).getId())));
    }

    @PostMapping("/{terminalId}/sessions")
    public ResponseEntity<ApiResponse<TerminalSessionResponseDto>> createSession(
            @PathVariable String terminalId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) UUID cashierId) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : "";
        var terminal = registrationService.getTerminalEntity(terminalId);
        var session = sessionService.createSession(terminal, token,
                null, null, cashierId,
                TerminalSessionService.DEFAULT_SESSION_TIMEOUT_MINUTES);
        return ResponseEntity.ok(ApiResponse.created(session));
    }

    @PostMapping("/{terminalId}/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@PathVariable String terminalId) {
        sessionService.expireAllSessions(registrationService.getTerminalEntity(terminalId).getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "All sessions expired"));
    }

    @GetMapping("/outdated")
    public ResponseEntity<ApiResponse<List<TerminalResponseDto>>> listOutdated() {
        List<TerminalResponseDto> outdated = registrationService.listAll().stream()
                .filter(t -> t.getMinimumBackendVersion() != null
                        && t.getAppVersion() != null
                        && compareVersions(t.getAppVersion(), t.getMinimumBackendVersion()) < 0)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(outdated));
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
