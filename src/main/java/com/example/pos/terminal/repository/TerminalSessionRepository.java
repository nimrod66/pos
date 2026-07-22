package com.example.pos.terminal.repository;

import com.example.pos.terminal.model.TerminalSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerminalSessionRepository extends JpaRepository<TerminalSession, Long> {
    Optional<TerminalSession> findBySessionId(String sessionId);
    Optional<TerminalSession> findByToken(String token);
    List<TerminalSession> findByTerminalIdAndActive(Long terminalId, boolean active);
    List<TerminalSession> findByActive(boolean active);
    long countByTerminalIdAndActive(Long terminalId, boolean active);
}
