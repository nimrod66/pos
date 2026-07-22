package com.example.pos.terminal.dto;

import com.example.pos.terminal.model.TerminalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalRegisterRequestDto {

    @NotBlank(message = "Terminal name is required")
    private String name;

    @NotNull(message = "Terminal type is required")
    private TerminalType terminalType;

    private String manufacturer;
    private String model;
    private String serialNumber;
    private String platform;
    private String osVersion;
    private String firmwareVersion;

    @NotNull(message = "Branch ID is required")
    private Long branchId;
}
