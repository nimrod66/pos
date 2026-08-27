package com.example.pos.core.backup.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.InternalServerException;
import com.example.pos.core.backup.dto.BackupResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final String dbName;
    private final String dbUser;
    private final String dbHost;
    private final int dbPort;
    private final String dbPassword;
    private final Path backupDir;
    private final int retentionDays;
    private final AtomicReference<Instant> lastBackupTime = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<String> lastBackupStatus = new AtomicReference<>("NONE");
    private final AtomicReference<String> lastBackupError = new AtomicReference<>(null);

    public BackupService(
            @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/pharmacy_pos}") String jdbcUrl,
            @Value("${spring.datasource.username:pharmacy_pos}") String dbUser,
            @Value("${spring.datasource.password:pharmacy_pos}") String dbPassword,
            @Value("${pos.backup.dir:backups}") String backupDir,
            @Value("${pos.backup.retention-days:30}") int retentionDays) {
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbHost = extractHost(jdbcUrl);
        this.dbPort = extractPort(jdbcUrl);
        this.dbName = extractDbName(jdbcUrl);
        this.backupDir = Path.of(backupDir).toAbsolutePath();
        this.retentionDays = retentionDays;
        try {
            Files.createDirectories(this.backupDir);
        } catch (IOException e) {
            throw new InternalServerException("Cannot create backup directory: " + this.backupDir);
        }
    }

    public BackupResponseDto createBackup() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault()).format(Instant.now());
        String filename = "pharmacy_pos_" + timestamp + ".dump";
        Path dumpPath = backupDir.resolve(filename);

        log.info("Starting backup to {}", dumpPath);

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-Fc",
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", dbName,
                "-f", dumpPath.toString()
        );
        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        try {
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                log.error("pg_dump failed: {}", output);
                throw new InternalServerException("Backup failed: " + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new InternalServerException("Backup process failed: " + e.getMessage());
        }

        try {
            long size = Files.size(dumpPath);
            log.info("Backup completed: {} ({} bytes)", filename, size);
            return new BackupResponseDto(filename, size, Instant.now(), "SUCCESS");
        } catch (IOException e) {
            throw new InternalServerException("Backup created but cannot read size: " + e.getMessage());
        }
    }

    public List<BackupResponseDto> listBackups() {
        try (Stream<Path> files = Files.list(backupDir)) {
            return files
                    .filter(p -> p.toString().endsWith(".dump"))
                    .sorted(Comparator.reverseOrder())
                    .map(p -> {
                        try {
                            String name = p.getFileName().toString();
                            long size = Files.size(p);
                            Instant created = Files.getLastModifiedTime(p).toInstant();
                            return new BackupResponseDto(name, size, created, "AVAILABLE");
                        } catch (IOException e) {
                            return new BackupResponseDto(p.getFileName().toString(), 0, Instant.now(), "ERROR");
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new InternalServerException("Cannot list backups: " + e.getMessage());
        }
    }

    public Path getBackupPath(String filename) {
        Path path = backupDir.resolve(filename).normalize();
        if (!path.startsWith(backupDir) || !Files.exists(path)) {
            throw new BadRequestException("Backup not found: " + filename);
        }
        return path;
    }

    public void restoreBackup(InputStream dumpStream) {
        Path tempDump = backupDir.resolve("restore_temp_" + System.currentTimeMillis() + ".dump");
        try {
            Files.copy(dumpStream, tempDump, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Failed to receive backup file: " + e.getMessage());
        }

        log.info("Starting restore from {}", tempDump);

        ProcessBuilder dropPb = new ProcessBuilder(
                "psql",
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", "postgres",
                "-c", "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + dbName + "' AND pid <> pg_backend_pid();",
                "-c", "DROP DATABASE IF EXISTS " + dbName + ";",
                "-c", "CREATE DATABASE " + dbName + " OWNER " + dbUser + ";"
        );
        dropPb.environment().put("PGPASSWORD", dbPassword);
        dropPb.redirectErrorStream(true);

        try {
            Process proc = dropPb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                log.error("Database reset failed: {}", output);
                throw new InternalServerException("Database reset failed: " + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new InternalServerException("Database reset process failed: " + e.getMessage());
        } finally {
            safeDelete(tempDump);
        }

        ProcessBuilder restorePb = new ProcessBuilder(
                "pg_restore",
                "--no-owner",
                "--role=" + dbUser,
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", dbName,
                tempDump.toString()
        );
        restorePb.environment().put("PGPASSWORD", dbPassword);
        restorePb.redirectErrorStream(true);

        try {
            Process proc = restorePb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                log.error("pg_restore warnings/errors: {}", output);
            }
            log.info("Restore completed successfully");
        } catch (IOException | InterruptedException e) {
            throw new InternalServerException("Restore process failed: " + e.getMessage());
        }
    }

    public void pruneOldBackups(int keepDays) {
        try (Stream<Path> files = Files.list(backupDir)) {
            long cutoff = System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000);
            files.filter(p -> p.toString().endsWith(".dump"))
                    .filter(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis() < cutoff; }
                        catch (IOException e) { return false; }
                    })
                    .forEach(this::safeDelete);
        } catch (IOException e) {
            log.warn("Prune failed: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledBackup() {
        log.info("Starting scheduled backup...");
        try {
            BackupResponseDto result = createBackup();
            lastBackupTime.set(Instant.now());
            lastBackupStatus.set("SUCCESS");
            lastBackupError.set(null);
            log.info("Scheduled backup completed: {}", result.getFilename());
            pruneOldBackups(retentionDays);
            log.info("Pruned backups older than {} days", retentionDays);
        } catch (Exception e) {
            lastBackupTime.set(Instant.now());
            lastBackupStatus.set("FAILED");
            lastBackupError.set(e.getMessage());
            log.error("Scheduled backup failed: {}", e.getMessage());
        }
    }

    public Map<String, Object> getBackupHealth() {
        List<BackupResponseDto> backups = listBackups();
        Instant lastTime = lastBackupTime.get();
        String status = lastBackupStatus.get();
        String error = lastBackupError.get();
        long totalSize = backups.stream().mapToLong(BackupResponseDto::getSizeBytes).sum();
        return Map.of(
            "status", status,
            "lastBackupTime", lastTime.toString(),
            "lastBackupError", error != null ? error : "",
            "backupCount", backups.size(),
            "totalSizeBytes", totalSize,
            "retentionDays", retentionDays
        );
    }

    private void safeDelete(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { log.warn("Cannot delete {}: {}", path, e.getMessage()); }
    }

    private static String extractHost(String jdbcUrl) {
        String withoutPrefix = jdbcUrl.replaceAll(".*//", "");
        return withoutPrefix.split(":")[0].split("/")[0];
    }

    private static int extractPort(String jdbcUrl) {
        String withoutPrefix = jdbcUrl.replaceAll(".*//", "");
        String hostPort = withoutPrefix.split("/")[0];
        String[] parts = hostPort.split(":");
        return parts.length > 1 ? Integer.parseInt(parts[1]) : 5432;
    }

    private static String extractDbName(String jdbcUrl) {
        String withoutPrefix = jdbcUrl.replaceAll(".*//", "");
        String path = withoutPrefix.contains("/") ? withoutPrefix.split("/", 2)[1] : "";
        String dbName = path.split("\\?")[0];
        return dbName.isEmpty() ? "pharmacy_pos" : dbName;
    }
}
