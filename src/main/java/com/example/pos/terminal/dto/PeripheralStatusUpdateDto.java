package com.example.pos.terminal.dto;

import com.example.pos.terminal.model.PeripheralStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeripheralStatusUpdateDto {
    @NotNull
    private PeripheralStatus status;
}
