package com.example.pos.terminal.dto;

import com.example.pos.terminal.model.TerminalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalStatusUpdateDto {

    @NotNull(message = "Status is required")
    private TerminalStatus status;
}
