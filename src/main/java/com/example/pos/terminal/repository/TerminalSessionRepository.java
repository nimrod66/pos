package com.example.pos.terminal.repository;

import java.util.UUID;

import com.example.pos.terminal.model.TerminalSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalSessionRepository extends JpaRepository<TerminalSession, UUID> {
    Optional<TerminalSession> findBySessionId(String sessionId);
    Optional<TerminalSession> findByToken(String token);
    List<TerminalSession> findByTerminalIdAndActive(UUID terminalId, boolean active);
    List<TerminalSession> findByActive(boolean active);
    long countByTerminalIdAndActive(UUID terminalId, boolean active);
}
