package com.example.pos.core.systemsettings.dto;

import com.example.pos.core.systemsettings.model.SystemSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsResponseDto {

    /** Values with these keys are never returned to any client. */
    public static final Set<String> SECRET_KEYS = Set.of(
            "payment.mpesa_consumer_secret",
            "payment.mpesa_passkey",
            "etims.signing_key");

    public static final String MASK = "********";

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

    public static boolean isSecretKey(String settingKey) {
        return settingKey != null && SECRET_KEYS.contains(settingKey);
    }

    public static SystemSettingsResponseDto from(SystemSettings settings) {
        return SystemSettingsResponseDto.builder()
                .id(settings.getId())
                .settingKey(settings.getSettingKey())
                .settingValue(isSecretKey(settings.getSettingKey()) ? MASK : settings.getSettingValue())
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
