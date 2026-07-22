package com.example.pos.terminal.dto;

import com.example.pos.terminal.model.ConnectionType;
import com.example.pos.terminal.model.PeripheralType;
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
public class HardwarePeripheralRequestDto {

    @NotNull(message = "Peripheral type is required")
    private PeripheralType type;

    private String manufacturer;
    private String model;

    @NotNull(message = "Connection type is required")
    private ConnectionType connectionType;

    private String configuration;
}
