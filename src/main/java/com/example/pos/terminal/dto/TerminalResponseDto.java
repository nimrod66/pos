package com.example.pos.terminal.dto;

import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.model.TerminalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalResponseDto {

    private UUID id;
    private String terminalId;
    private String name;
    private TerminalType terminalType;
    private String manufacturer;
    private String model;
    private String serialNumber;
    private String platform;
    private String osVersion;
    private String firmwareVersion;
    private TerminalStatus status;
    private UUID branchId;
    private String registeredBy;
    private LocalDateTime registeredAt;
    private LocalDateTime lastSeenAt;
    private String appVersion;
    private String supportedApiVersion;
    private LocalDateTime lastUpdate;
    private String minimumBackendVersion;
    private boolean migratedFromTerminal;
    private List<HardwarePeripheralDto> peripherals;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

