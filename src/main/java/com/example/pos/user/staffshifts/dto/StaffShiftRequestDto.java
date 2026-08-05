package com.example.pos.user.staffshifts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffShiftRequestDto {

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private UUID roleId;

    @NotBlank(message = "Shift name is required")
    private String shiftName;

    private Integer shiftNumber;

    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;

    private String remarks;
}

