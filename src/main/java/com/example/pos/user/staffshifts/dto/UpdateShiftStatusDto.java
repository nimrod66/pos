package com.example.pos.user.staffshifts.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShiftStatusDto {

    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;
}
