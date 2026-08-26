package com.example.pos.core.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponseDto {

    private String filename;
    private long sizeBytes;
    private Instant createdAt;
    private String status;
}
