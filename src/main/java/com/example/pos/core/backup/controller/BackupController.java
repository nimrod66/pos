package com.example.pos.core.backup.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.backup.dto.BackupResponseDto;
import com.example.pos.core.backup.service.BackupService;
import com.example.pos.operations.model.OperationalMetricEvent;
import com.example.pos.operations.service.OperationalMetricsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/backup")
@PreAuthorize("hasAuthority('settings.manage')")
public class BackupController {

    private final BackupService backupService;
    private final OperationalMetricsService metricsService;

    public BackupController(BackupService backupService, OperationalMetricsService metricsService) {
        this.backupService = backupService;
        this.metricsService = metricsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BackupResponseDto>> createBackup() {
        long started = System.nanoTime();
        metricsService.record(OperationalMetricEvent.EventType.BACKUP,
                OperationalMetricEvent.EventStatus.ATTEMPTED, null, "backup-api", null, null, null, null, null);
        try {
            BackupResponseDto result = backupService.createBackup();
            metricsService.record(OperationalMetricEvent.EventType.BACKUP,
                    OperationalMetricEvent.EventStatus.SUCCESS, "BACKUP_CREATED", "backup-api", null,
                    null, null, elapsedMs(started), result.getFilename());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (RuntimeException ex) {
            metricsService.record(OperationalMetricEvent.EventType.BACKUP,
                    OperationalMetricEvent.EventStatus.FAILED, ex.getClass().getSimpleName(), "backup-api", null,
                    null, null, elapsedMs(started), ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<BackupResponseDto>>> listBackups() {
        return ResponseEntity.ok(ApiResponse.ok(backupService.listBackups()));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) {
        Path path = backupService.getBackupPath(filename);
        FileSystemResource resource = new FileSystemResource(path.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(path.toFile().length())
                .body(resource);
    }

    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirm", defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Set confirm=true to proceed with restore. This will REPLACE the current database."));
        }
        long started = System.nanoTime();
        metricsService.record(OperationalMetricEvent.EventType.RESTORE,
                OperationalMetricEvent.EventStatus.ATTEMPTED, null, "backup-api", null, null, null, null, file.getOriginalFilename());
        try {
            backupService.restoreBackup(file.getInputStream());
            metricsService.record(OperationalMetricEvent.EventType.RESTORE,
                    OperationalMetricEvent.EventStatus.SUCCESS, "RESTORE_COMPLETED", "backup-api", null,
                    null, null, elapsedMs(started), file.getOriginalFilename());
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IOException e) {
            metricsService.record(OperationalMetricEvent.EventType.RESTORE,
                    OperationalMetricEvent.EventStatus.FAILED, "RESTORE_UPLOAD_READ_FAILED", "backup-api", null,
                    null, null, elapsedMs(started), e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to read uploaded file: " + e.getMessage()));
        } catch (RuntimeException e) {
            metricsService.record(OperationalMetricEvent.EventType.RESTORE,
                    OperationalMetricEvent.EventStatus.FAILED, e.getClass().getSimpleName(), "backup-api", null,
                    null, null, elapsedMs(started), e.getMessage());
            throw e;
        }
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(@PathVariable String filename) {
        Path path = backupService.getBackupPath(filename);
        try {
            java.nio.file.Files.deleteIfExists(path);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Cannot delete backup: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> backupHealth() {
        return ResponseEntity.ok(ApiResponse.ok(backupService.getBackupHealth()));
    }
}
