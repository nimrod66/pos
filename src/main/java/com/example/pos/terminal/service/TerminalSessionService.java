package com.example.pos.terminal.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.terminal.dto.TerminalSessionResponseDto;
import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalSession;
import com.example.pos.terminal.repository.TerminalSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TerminalSessionService {

    private final TerminalSessionRepository sessionRepository;

    public static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 480;

    public TerminalSessionResponseDto createSession(Terminal terminal, String token,
                                                     String ipAddress, String userAgent,
                                                     Long cashierId, int timeoutMinutes) {
        int maxSessions = 5;
        long activeCount = sessionRepository.countByTerminalIdAndActive(terminal.getId(), true);
        if (activeCount >= maxSessions) {
            expireOldestSessions(terminal.getId(), (int) (activeCount - maxSessions + 1));
        }

        TerminalSession session = TerminalSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .terminal(terminal)
                .token(token)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .cashierId(cashierId)
                .lastActivityAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(timeoutMinutes))
                .active(true)
                .build();

        session = sessionRepository.save(session);
        log.debug("Session created for terminal {}: {}", terminal.getTerminalId(), session.getSessionId());
        return toDto(session);
    }

    @Transactional(readOnly = true)
    public boolean validateSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .map(session -> {
                    if (!session.isActive()) return false;
                    if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return sessionRepository.findByToken(token)
                .map(session -> {
                    if (!session.isActive()) return false;
                    if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    public void refreshSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setLastActivityAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    public void expireSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setActive(false);
            sessionRepository.save(session);
        });
    }

    public void expireAllSessions(Long terminalId) {
        List<TerminalSession> sessions = sessionRepository.findByTerminalIdAndActive(terminalId, true);
        sessions.forEach(s -> s.setActive(false));
        sessionRepository.saveAll(sessions);
    }

    public void expireOldestSessions(Long terminalId, int count) {
        List<TerminalSession> sessions = sessionRepository.findByTerminalIdAndActive(terminalId, true);
        sessions.stream()
                .sorted((a, b) -> a.getLastActivityAt().compareTo(b.getLastActivityAt()))
                .limit(count)
                .forEach(s -> {
                    s.setActive(false);
                    sessionRepository.save(s);
                });
    }

    @Transactional(readOnly = true)
    public List<TerminalSessionResponseDto> getActiveSessions(Long terminalId) {
        return sessionRepository.findByTerminalIdAndActive(terminalId, true).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private TerminalSessionResponseDto toDto(TerminalSession session) {
        return TerminalSessionResponseDto.builder()
                .sessionId(session.getSessionId())
                .terminalId(session.getTerminal().getTerminalId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .cashierId(session.getCashierId())
                .createdAt(session.getCreatedAt())
                .lastActivityAt(session.getLastActivityAt())
                .expiresAt(session.getExpiresAt())
                .active(session.isActive())
                .build();
    }
}
