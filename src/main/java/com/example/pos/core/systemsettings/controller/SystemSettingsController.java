package com.example.pos.core.systemsettings.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.systemsettings.dto.SystemSettingsRequestDto;
import com.example.pos.core.systemsettings.dto.SystemSettingsResponseDto;
import com.example.pos.core.systemsettings.model.SystemSettings;
import com.example.pos.core.systemsettings.service.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    public SystemSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemSettingsResponseDto>> create(
            @RequestBody @Valid SystemSettingsRequestDto dto) {
        SystemSettings settings = settingsService.createSetting(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(SystemSettingsResponseDto.from(settings)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettingsResponseDto>>> getAll(
            @RequestParam(required = false) Long pharmacyId,
            @RequestParam(required = false) Long branchId) {
        List<SystemSettings> settings;
        if (branchId != null) {
            settings = settingsService.getSettingsByBranch(branchId);
        } else if (pharmacyId != null) {
            settings = settingsService.getSettingsByPharmacy(pharmacyId);
        } else {
            settings = settingsService.getSettingsByPharmacy(null);
        }
        List<SystemSettingsResponseDto> response = settings.stream()
                .map(SystemSettingsResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<SystemSettingsResponseDto>> resolve(
            @RequestParam String key,
            @RequestParam(required = false) Long branchId,
            @RequestParam Long pharmacyId) {
        SystemSettings setting = settingsService.resolveSetting(key, branchId, pharmacyId);
        if (setting == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "No setting found"));
        }
        return ResponseEntity.ok(ApiResponse.ok(SystemSettingsResponseDto.from(setting)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemSettingsResponseDto>> getById(@PathVariable Long id) {
        SystemSettings settings = settingsService.getSettingById(id);
        return ResponseEntity.ok(ApiResponse.ok(SystemSettingsResponseDto.from(settings)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemSettingsResponseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid SystemSettingsRequestDto dto) {
        SystemSettings settings = settingsService.updateSetting(id, dto);
        return ResponseEntity.ok(ApiResponse.updated(SystemSettingsResponseDto.from(settings)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        settingsService.deleteSetting(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
