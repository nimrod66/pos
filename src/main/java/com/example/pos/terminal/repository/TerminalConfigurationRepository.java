package com.example.pos.terminal.repository;

import com.example.pos.terminal.model.TerminalConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalConfigurationRepository extends JpaRepository<TerminalConfiguration, Long> {
    List<TerminalConfiguration> findByTerminalId(Long terminalId);
    Optional<TerminalConfiguration> findByTerminalIdAndConfigKey(Long terminalId, String configKey);
    void deleteAllByTerminalId(Long terminalId);
}
