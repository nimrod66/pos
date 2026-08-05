package com.example.pos.terminal.repository;

import java.util.UUID;

import com.example.pos.terminal.model.TerminalConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalConfigurationRepository extends JpaRepository<TerminalConfiguration, UUID> {
    List<TerminalConfiguration> findByTerminalId(UUID terminalId);
    Optional<TerminalConfiguration> findByTerminalIdAndConfigKey(UUID terminalId, String configKey);
    void deleteAllByTerminalId(UUID terminalId);
}
