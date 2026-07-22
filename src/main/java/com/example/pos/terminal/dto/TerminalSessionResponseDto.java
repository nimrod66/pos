package com.example.pos.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalSessionResponseDto {

    private String sessionId;
    private String terminalId;
    private String token;
    private String ipAddress;
    private String userAgent;
    private Long cashierId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime expiresAt;
    private boolean active;
}
