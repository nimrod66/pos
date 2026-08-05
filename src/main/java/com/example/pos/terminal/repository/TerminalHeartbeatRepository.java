package com.example.pos.terminal.repository;

import java.util.UUID;

import com.example.pos.terminal.model.TerminalHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalHeartbeatRepository extends JpaRepository<TerminalHeartbeat, UUID> {
    List<TerminalHeartbeat> findByTerminalIdOrderByTimestampDesc(UUID terminalId);
}
