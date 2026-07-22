package com.example.pos.terminal.repository;

import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.model.TerminalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalRegistryRepository extends JpaRepository<Terminal, Long> {
    Optional<Terminal> findByTerminalId(String terminalId);
    Optional<Terminal> findByName(String name);
    Optional<Terminal> findByApiKey(String apiKey);
    List<Terminal> findByStatus(TerminalStatus status);
    List<Terminal> findByTerminalType(TerminalType terminalType);
    List<Terminal> findByBranchId(Long branchId);
    List<Terminal> findByStatusAndTerminalType(TerminalStatus status, TerminalType terminalType);
    boolean existsByTerminalId(String terminalId);
    boolean existsByName(String name);
}
