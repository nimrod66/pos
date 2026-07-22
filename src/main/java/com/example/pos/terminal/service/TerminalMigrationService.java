package com.example.pos.terminal.service;

import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TerminalMigrationService implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final TerminalRegistryRepository terminalRepository;

    @Override
    public void run(String... args) {
        if (needsMigration()) {
            migrate();
        }
    }

    private boolean needsMigration() {
        try {
            Integer oldCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM terminals WHERE terminal_id IS NOT NULL", Integer.class);
            if (oldCount == null || oldCount == 0) {
                return false;
            }
            Long newCount = terminalRepository.count();
            return newCount < oldCount;
        } catch (Exception e) {
            log.debug("Migration check skipped: {}", e.getMessage());
            return false;
        }
    }

    private void migrate() {
        log.info("Starting migration from terminals to terminal_registry...");
        List<Map<String, Object>> oldTerminals = jdbcTemplate.queryForList(
                "SELECT terminal_id, name, api_key, api_secret, active, branch_id, registered_at FROM terminals");

        int migrated = 0;
        for (Map<String, Object> row : oldTerminals) {
            String terminalId = (String) row.get("terminal_id");
            if (terminalRepository.existsByTerminalId(terminalId)) {
                continue;
            }

            boolean active = (Boolean) row.getOrDefault("active", false);
            Terminal terminal = Terminal.builder()
                    .terminalId(terminalId)
                    .name((String) row.get("name"))
                    .apiKey((String) row.get("api_key"))
                    .apiSecret((String) row.get("api_secret"))
                    .status(active ? TerminalStatus.ACTIVE : TerminalStatus.PENDING)
                    .branchId(toLong(row.get("branch_id")))
                    .migratedFromTerminal(true)
                    .build();

            terminalRepository.save(terminal);
            migrated++;
        }

        log.info("Migration complete: {} terminals migrated to terminal_registry", migrated);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
