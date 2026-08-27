package com.example.pos.terminal.repository;

import java.util.UUID;

import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.model.TerminalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TerminalRegistryRepository extends JpaRepository<Terminal, UUID> {
    Optional<Terminal> findByTerminalId(String terminalId);
    Optional<Terminal> findByName(String name);
    Optional<Terminal> findByApiKey(String apiKey);
    List<Terminal> findByStatus(TerminalStatus status);
    List<Terminal> findByTerminalType(TerminalType terminalType);
    List<Terminal> findByBranchId(UUID branchId);
    List<Terminal> findByBranchIdIn(Collection<UUID> branchIds);
    List<Terminal> findByBranchIdInAndStatus(Collection<UUID> branchIds, TerminalStatus status);
    List<Terminal> findByStatusAndTerminalType(TerminalStatus status, TerminalType terminalType);
    boolean existsByTerminalId(String terminalId);
    boolean existsByName(String name);
    boolean existsByBranchIdAndNameIgnoreCase(UUID branchId, String name);
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(UUID branchId, String name, UUID id);

    long countByStatus(TerminalStatus status);
}
