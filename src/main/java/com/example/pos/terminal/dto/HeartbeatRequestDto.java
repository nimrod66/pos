package com.example.pos.terminal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatRequestDto {

    @NotNull(message = "Terminal ID is required")
    private String terminalId;

    private Integer batteryLevel;
    private Boolean batteryCharging;
    private String networkType;
    private Integer signalStrength;
    private Long uptimeMinutes;
    private String peripheralStatus;
    private String additionalMetrics;
    private LocalDateTime timestamp;
}
