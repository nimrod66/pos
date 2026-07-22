package com.example.pos.terminal.dto;

import com.example.pos.terminal.model.ConnectionType;
import com.example.pos.terminal.model.PeripheralStatus;
import com.example.pos.terminal.model.PeripheralType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HardwarePeripheralDto {

    private Long id;
    private PeripheralType type;
    private String manufacturer;
    private String model;
    private ConnectionType connectionType;
    private PeripheralStatus status;
    private String configuration;
}
