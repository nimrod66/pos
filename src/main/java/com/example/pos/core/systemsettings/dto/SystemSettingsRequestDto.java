package com.example.pos.core.systemsettings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsRequestDto {

    @NotBlank(message = "Setting key is required")
    private String settingKey;

    private String settingValue;

    private String description;

    private UUID branchId;

    @NotNull(message = "Pharmacy ID is required")
    private UUID pharmacyId;
}

