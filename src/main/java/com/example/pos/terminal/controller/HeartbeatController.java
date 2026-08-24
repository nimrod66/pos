package com.example.pos.terminal.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.terminal.dto.HeartbeatRequestDto;
import com.example.pos.terminal.dto.TerminalHealthDto;
import com.example.pos.terminal.service.TerminalHeartbeatService;
import com.example.pos.terminal.service.TerminalRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/terminals")
@RequiredArgsConstructor
public class HeartbeatController {

    private final TerminalHeartbeatService heartbeatService;
    private final TerminalRegistrationService registrationService;

    @PostMapping("/{terminalId}/heartbeat")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell', 'inventory.read')")
    public ResponseEntity<ApiResponse<Map<String, String>>> heartbeat(
            @PathVariable String terminalId, @Valid @RequestBody HeartbeatRequestDto request) {
        if (!terminalId.equals(request.getTerminalId())) {
            request.setTerminalId(terminalId);
        }
        registrationService.getByTerminalId(terminalId);
        heartbeatService.receiveHeartbeat(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "received")));
    }

    @GetMapping("/{terminalId}/health")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell', 'inventory.read')")
    public ResponseEntity<ApiResponse<TerminalHealthDto>> health(@PathVariable String terminalId) {
        boolean online = heartbeatService.isTerminalOnline(terminalId, 10);
        var terminal = registrationService.getByTerminalId(terminalId);
        return ResponseEntity.ok(ApiResponse.ok(TerminalHealthDto.builder()
                .terminalId(terminalId)
                .online(online)
                .lastSeenAt(terminal.getLastSeenAt())
                .peripherals(terminal.getPeripherals())
                .build()));
    }
}
