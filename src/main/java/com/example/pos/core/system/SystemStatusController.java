package com.example.pos.core.system;

import com.example.pos.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final JdbcTemplate jdbcTemplate;
    private final String applicationName;

    public SystemStatusController(JdbcTemplate jdbcTemplate,
                                  @Value("${spring.application.name:POS}") String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
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

    public record SystemStatusResponse(
            String application,
            String api,
            String database,
            String databaseName,
            String version,
            Instant checkedAt) {
    }
}
