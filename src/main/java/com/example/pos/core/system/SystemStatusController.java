package com.example.pos.core.system;

import com.example.pos.common.dto.ApiResponse;
import com.example.pos.core.backup.service.BackupService;
import com.example.pos.sync.service.ConnectivityService;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final BackupService backupService;
    private final ConnectivityService connectivityService;
    private final TerminalRegistryRepository terminalRepository;
    private final String applicationName;

    private volatile Map<String, Object> cachedHealth;
    private volatile Instant lastHealthCheck = Instant.EPOCH;

    public SystemStatusController(JdbcTemplate jdbcTemplate,
                                  DataSource dataSource,
                                  BackupService backupService,
                                  ConnectivityService connectivityService,
                                  TerminalRegistryRepository terminalRepository,
                                  @Value("${spring.application.name:POS}") String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.backupService = backupService;
        this.connectivityService = connectivityService;
        this.terminalRepository = terminalRepository;
        this.applicationName = applicationName;
    }

    @GetMapping("/status")
    public ApiResponse<SystemStatusResponse> status() {
        String databaseName = jdbcTemplate.queryForObject("select current_database()", String.class);
        String version = SystemStatusController.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            version = "0.0.1-SNAPSHOT";
        }
        return ApiResponse.ok(new SystemStatusResponse(
                applicationName, "UP", "UP", databaseName, version, Instant.now()));
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        if (cachedHealth != null && lastHealthCheck.isAfter(Instant.now().minusSeconds(15))) {
            return ApiResponse.ok(cachedHealth);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", Instant.now());
        result.put("api", checkApi());
        result.put("database", checkDatabase());
        result.put("disk", checkDisk());
        result.put("memory", checkMemory());
        result.put("connectionPool", checkConnectionPool());
        result.put("backup", checkBackup());
        result.put("sync", checkSync());
        result.put("terminals", checkTerminals());

        String overall = "HEALTHY";
        for (Object v : result.values()) {
            if (v instanceof Map<?, ?> m && "DOWN".equals(m.get("status"))) {
                overall = "DEGRADED";
                break;
            }
        }
        result.put("status", overall);
        cachedHealth = result;
        lastHealthCheck = Instant.now();
        return ApiResponse.ok(result);
    }

    private Map<String, Object> checkApi() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        m.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + "s");
        return m;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            String db = jdbcTemplate.queryForObject("select current_database()", String.class);
            m.put("status", "UP");
            m.put("database", db);
            Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM medicine", Long.class);
            m.put("medicineCount", count);
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> checkDisk() {
        Map<String, Object> m = new LinkedHashMap<>();
        File root = new File("/");
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        long used = total - free;
        m.put("totalGB", total / (1024 * 1024 * 1024));
        m.put("freeGB", free / (1024 * 1024 * 1024));
        m.put("usedGB", used / (1024 * 1024 * 1024));
        long pct = total > 0 ? (used * 100 / total) : 0;
        m.put("usedPercent", pct);
        m.put("status", pct > 90 ? "DOWN" : pct > 80 ? "WARNING" : "UP");
        return m;
    }

    private Map<String, Object> checkMemory() {
        Map<String, Object> m = new LinkedHashMap<>();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mem.getHeapMemoryUsage();
        long usedMB = heap.getUsed() / (1024 * 1024);
        long maxMB = heap.getMax() / (1024 * 1024);
        m.put("heapUsedMB", usedMB);
        m.put("heapMaxMB", maxMB);
        long pct = maxMB > 0 ? (usedMB * 100 / maxMB) : 0;
        m.put("heapUsedPercent", pct);
        m.put("status", pct > 90 ? "DOWN" : pct > 80 ? "WARNING" : "UP");
        return m;
    }

    private Map<String, Object> checkConnectionPool() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (dataSource instanceof HikariDataSource hikari) {
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool != null) {
                m.put("totalConnections", pool.getTotalConnections());
                m.put("activeConnections", pool.getActiveConnections());
                m.put("idleConnections", pool.getIdleConnections());
                m.put("threadsAwaiting", pool.getThreadsAwaitingConnection());
                m.put("status", pool.getActiveConnections() < hikari.getMaximumPoolSize() ? "UP" : "WARNING");
            } else {
                m.put("status", "UNKNOWN");
            }
        } else {
            m.put("status", "UNKNOWN");
        }
        return m;
    }

    private Map<String, Object> checkBackup() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            var backups = backupService.listBackups();
            m.put("count", backups.size());
            if (!backups.isEmpty()) {
                var latest = backups.get(0);
                m.put("lastBackup", latest.getCreatedAt());
                m.put("lastBackupSize", latest.getSizeBytes());
                long hoursSince = java.time.Duration.between(latest.getCreatedAt(), Instant.now()).toHours();
                m.put("hoursSinceBackup", hoursSince);
                m.put("status", hoursSince > 48 ? "WARNING" : "UP");
            } else {
                m.put("status", "WARNING");
                m.put("lastBackup", null);
            }
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> checkSync() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Map<String, Object> status = connectivityService.getStatus();
            Boolean online = status != null ? (Boolean) status.get("connected") : null;
            m.put("online", online != null && online);
            m.put("latencyMs", status != null ? status.get("lastLatencyMs") : null);
            m.put("mode", status != null ? status.get("mode") : "UNKNOWN");
            m.put("status", Boolean.TRUE.equals(online) ? "UP" : "WARNING");
        } catch (Exception e) {
            m.put("status", "UNKNOWN");
        }
        return m;
    }

    private Map<String, Object> checkTerminals() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            long total = terminalRepository.count();
            m.put("total", total);
            long active = terminalRepository.countByStatus(com.example.pos.terminal.model.TerminalStatus.ACTIVE);
            m.put("active", active);
            m.put("status", "UP");
        } catch (Exception e) {
            m.put("status", "UNKNOWN");
        }
        return m;
    }

    public record SystemStatusResponse(
            String application,
            String api,
            String database,
            String databaseName,
            String version,
            Instant checkedAt) {
    }
}
