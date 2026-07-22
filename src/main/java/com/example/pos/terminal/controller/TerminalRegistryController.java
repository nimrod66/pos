package com.example.pos.terminal.controller;

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

@RestController
@RequestMapping("/api/terminals")
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
    public ResponseEntity<ApiResponse<Void>> removePeripheral(@PathVariable Long peripheralId) {
        registrationService.removePeripheral(peripheralId);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @GetMapping("/{terminalId}/sessions")
    public ResponseEntity<ApiResponse<List<TerminalSessionResponseDto>>> getSessions(@PathVariable String terminalId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getActiveSessions(
                registrationService.getTerminalEntity(terminalId).getId())));
    }

    @PostMapping("/{terminalId}/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@PathVariable String terminalId) {
        sessionService.expireAllSessions(registrationService.getTerminalEntity(terminalId).getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "All sessions expired"));
    }
}
