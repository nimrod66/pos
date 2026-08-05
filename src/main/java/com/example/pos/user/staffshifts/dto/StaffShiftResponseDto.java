package com.example.pos.user.staffshifts.dto;

import com.example.pos.user.staffshifts.model.StaffShifts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffShiftResponseDto {

    private UUID id;
    private String shiftName;
    private Integer shiftNumber;
    private String status;
    private UUID branchId;
    private String branchName;
    private UUID userId;
    private String userName;
    private UUID roleId;
    private String roleName;
    private LocalDateTime shiftStartTime;
    private LocalDateTime shiftEndTime;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffShiftResponseDto from(StaffShifts shift) {
        return StaffShiftResponseDto.builder()
                .id(shift.getId())
                .shiftName(shift.getShiftName())
                .shiftNumber(shift.getShiftNumber())
                .status(shift.getStatus() != null ? shift.getStatus().name() : null)
                .branchId(shift.getBranch() != null ? shift.getBranch().getId() : null)
                .branchName(shift.getBranch() != null ? shift.getBranch().getBranchName() : null)
                .userId(shift.getUser() != null ? shift.getUser().getId() : null)
                .userName(shift.getUser() != null
                        ? shift.getUser().getFirstName() + " " + shift.getUser().getLastName()
                        : null)
                .roleId(shift.getUserRoles() != null ? shift.getUserRoles().getId() : null)
                .roleName(shift.getUserRoles() != null ? shift.getUserRoles().getRoleName() : null)
                .shiftStartTime(shift.getShiftStartTime())
                .shiftEndTime(shift.getShiftEndTime())
                .remarks(shift.getRemarks())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }
}

