package com.example.pos.terminal.auth;

import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalPrincipal {

    private String terminalId;
    private String name;
    private TerminalType terminalType;
    private String appVersion;
    private Long branchId;
    private boolean active;
}
