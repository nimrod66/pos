package com.example.pos.core.systemsettings.dto;

import com.example.pos.core.systemsettings.model.SystemSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsResponseDto {

    private UUID id;
    private String settingKey;
    private String settingValue;
    private String description;
    private UUID branchId;
    private String branchName;
    private UUID pharmacyId;
    private String pharmacyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SystemSettingsResponseDto from(SystemSettings settings) {
        return SystemSettingsResponseDto.builder()
                .id(settings.getId())
                .settingKey(settings.getSettingKey())
                .settingValue(settings.getSettingValue())
                .description(settings.getDescription())
                .branchId(settings.getBranch() != null ? settings.getBranch().getId() : null)
                .branchName(settings.getBranch() != null ? settings.getBranch().getBranchName() : null)
                .pharmacyId(settings.getPharmacy() != null ? settings.getPharmacy().getId() : null)
                .pharmacyName(settings.getPharmacy() != null ? settings.getPharmacy().getName() : null)
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}

