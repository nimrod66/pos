package com.example.pos.notification.dto;

import com.example.pos.notification.model.Notification;
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
public class NotificationResponseDto {

    private UUID id;
    private String title;
    private String message;
    private String type;
    private String status;
    private UUID referenceId;
    private String referenceType;
    private UUID branchId;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification n) {
        return NotificationResponseDto.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .status(n.getStatus() != null ? n.getStatus().name() : null)
                .referenceId(n.getReferenceId()).referenceType(n.getReferenceType())
                .branchId(n.getBranchId()).createdAt(n.getCreatedAt()).build();
    }
}

