package com.example.pos.pos.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/hardware")
public class HardwareBridgeController {

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell', 'inventory.read')")
    public ApiResponse<Map<String, String>> status() {
        return ApiResponse.ok(Map.of(
                "mode", "rest",
                "note", "Hardware is controlled via the Python connector service (localhost:9100). "
                        + "This endpoint provides the bridge configuration."
        ));
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell', 'inventory.read')")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.ok(Map.of(
                "connectorUrl", "http://localhost:9100",
                "endpoints", Map.of(
                        "print", "POST /print",
                        "cashDrawer", "POST /cash-drawer/open",
                        "display", "POST /display/show",
                        "health", "GET /health"
                ),
                "printerType", "esc_pos",
                "receiptWidth", 42,
                "scannerMode", "keyboard_wedge"
        ));
    }
}
