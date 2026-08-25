package com.example.pos.pos.controller;

import java.util.UUID;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.systemsettings.SettingKeys;
import com.example.pos.core.systemsettings.service.SystemSettingsService;
import com.example.pos.security.auth.AuthenticatedUserContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/hardware")
public class HardwareBridgeController {

    private static final String DEFAULT_CONNECTOR_URL = "http://localhost:9100";

    private final SystemSettingsService settingsService;
    private final AuthenticatedUserContext current;

    public HardwareBridgeController(SystemSettingsService settingsService,
                                    AuthenticatedUserContext current) {
        this.settingsService = settingsService;
        this.current = current;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell', 'inventory.read')")
    public ApiResponse<Map<String, String>> status() {
        return ApiResponse.ok(Map.of(
                "mode", "rest",
                "note", "Hardware is controlled via the Python connector service. "
                        + "This endpoint provides the bridge configuration."
        ));
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('terminal.read', 'terminal.manage', 'pos.sell', 'inventory.read')")
    public ApiResponse<Map<String, Object>> config() {
        return ApiResponse.ok(Map.of(
                "connectorUrl", connectorUrl(),
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

    private String connectorUrl() {
        try {
            UUID pharmacyId = current.pharmacyId();
            return settingsService.resolveSettingValue(
                    SettingKeys.Hardware.CONNECTOR_URL, null, pharmacyId, DEFAULT_CONNECTOR_URL);
        } catch (RuntimeException ex) {
            return DEFAULT_CONNECTOR_URL;
        }
    }
}
