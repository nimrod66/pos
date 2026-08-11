package com.example.pos.user.staffshifts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffShiftRequestDto {

    private UUID branchId;

    private UUID userId;

    private UUID roleId;

    private String shiftName;

    private Integer shiftNumber;

    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;

    @DecimalMin(value = "0.00")
    private BigDecimal openingFloat;

    private String remarks;
}

