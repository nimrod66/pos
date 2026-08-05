package com.example.pos.audit.dto;

import com.example.pos.audit.model.AuditLog;
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
public class AuditLogResponseDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private String tableName;
    private String recordId;
    private String action;
    private LocalDateTime createdAt;

    public static AuditLogResponseDto from(AuditLog log) {
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getFirstName() : null)
                .tableName(log.getTableName())
                .recordId(log.getRecordId())
                .action(log.getAction())
                .createdAt(log.getCreatedAt()).build();
    }
}

