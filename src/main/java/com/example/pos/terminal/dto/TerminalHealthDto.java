package com.example.pos.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalHealthDto {
    private String terminalId;
    private boolean online;
    private LocalDateTime lastSeenAt;
    private List<HardwarePeripheralDto> peripherals;
}
