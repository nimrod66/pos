package com.example.pos.sync.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class TerminalConfig {

    private static final Path CONFIG_DIR = Paths.get("pos-data");
    private static final Path ID_FILE = CONFIG_DIR.resolve("terminal.id");

    @Getter
    private String terminalId;

    private final SyncProperties syncProperties;

    public TerminalConfig(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(ID_FILE)) {
                terminalId = Files.readString(ID_FILE).trim();
            } else {
                terminalId = "TERM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Files.writeString(ID_FILE, terminalId);
            }
            log.info("Terminal ID: {} (sync mode: {})", terminalId, syncProperties.getMode().toUpperCase());
        } catch (IOException e) {
            terminalId = "TERM-UNKNOWN";
            log.error("Failed to read/write terminal ID", e);
        }
    }
}
