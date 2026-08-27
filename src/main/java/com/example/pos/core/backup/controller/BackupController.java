package com.example.pos.core.backup.controller;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.backup.dto.BackupResponseDto;
import com.example.pos.core.backup.service.BackupService;
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

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BackupResponseDto>> createBackup() {
        BackupResponseDto result = backupService.createBackup();
        return ResponseEntity.ok(ApiResponse.ok(result));
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
        try {
            backupService.restoreBackup(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to read uploaded file: " + e.getMessage()));
        }
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
