package com.example.pos.sync.service;

import com.example.pos.common.exception.BadRequestException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.sync.model.Terminal;
import com.example.pos.sync.model.TerminalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class TerminalService {

    private final TerminalRepository terminalRepository;
    private final SyncService syncService;

    public TerminalService(TerminalRepository terminalRepository, SyncService syncService) {
        this.terminalRepository = terminalRepository;
        this.syncService = syncService;
    }

    public Terminal registerTerminal(String terminalId, String name) {
        if (terminalRepository.existsByName(name)) {
            throw new BadRequestException("Terminal name already exists: " + name);
        }

        Terminal terminal = Terminal.builder()
                .terminalId("TERM-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .name(name)
                .apiKey(Terminal.generateApiKey())
                .apiSecret(Terminal.generateApiSecret())
                .active(true)
                .build();

        Terminal saved = terminalRepository.save(terminal);
        return saved;
    }

    public Terminal registerByLocalId(String terminalId, String name, String branchId) {
        if (terminalRepository.existsById(terminalId)) {
            throw new BadRequestException("Terminal already registered: " + terminalId);
        }

        Terminal terminal = Terminal.builder()
                .terminalId(terminalId)
                .name(name)
                .apiKey(Terminal.generateApiKey())
                .apiSecret(Terminal.generateApiSecret())
                .active(true)
                .branchId(branchId)
                .build();

        return terminalRepository.save(terminal);
    }

    public Terminal approveTerminal(String terminalId) {
        Terminal terminal = findById(terminalId);
        terminal.setActive(true);
        terminal.setSynced(true);
        Terminal saved = terminalRepository.save(terminal);
        autoRetryDeadEvents(terminalId);
        return saved;
    }

    public Terminal deactivateTerminal(String terminalId) {
        Terminal terminal = findById(terminalId);
        terminal.setActive(false);
        return terminalRepository.save(terminal);
    }

    public Terminal regenerateApiKey(String terminalId) {
        Terminal terminal = findById(terminalId);
        terminal.setApiKey(Terminal.generateApiKey());
        terminal.setApiSecret(Terminal.generateApiSecret());
        Terminal saved = terminalRepository.save(terminal);
        autoRetryDeadEvents(terminalId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Terminal findById(String terminalId) {
        return terminalRepository.findById(terminalId)
                .orElseThrow(() -> new ResourceNotFoundException("Terminal", terminalId));
    }

    @Transactional(readOnly = true)
    public List<Terminal> listAll() {
        return terminalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Terminal> listPending() {
        return terminalRepository.findBySynced(false);
    }

    @Transactional(readOnly = true)
    public List<Terminal> listActive() {
        return terminalRepository.findByActive(true);
    }

    public void markSynced(String terminalId) {
        Terminal terminal = findById(terminalId);
        terminal.setSynced(true);
        terminalRepository.save(terminal);
    }

    public Map<String, Object> getTerminalHealth(String terminalId) {
        Terminal terminal = findById(terminalId);
        Map<String, Object> health = new java.util.LinkedHashMap<>();
        health.put("terminalId", terminal.getTerminalId());
        health.put("name", terminal.getName());
        health.put("active", terminal.isActive());
        health.put("deadEventCount", syncService.getDeadEvents().stream()
                .filter(e -> terminal.getTerminalId().equals(e.getTerminalId()))
                .count());
        health.put("registeredAt", terminal.getRegisteredAt() != null
                ? terminal.getRegisteredAt().toString() : null);
        return health;
    }

    private void autoRetryDeadEvents(String terminalId) {
        try {
            Map<String, Object> result = syncService.retryDeadEventsForTerminal(terminalId);
            int retried = (int) result.getOrDefault("retried", 0);
            if (retried > 0) {
                log.info("Auto-retried {} dead events for terminal {}", retried, terminalId);
            }
        } catch (Exception e) {
            log.warn("Failed to auto-retry dead events for terminal {}: {}", terminalId, e.getMessage());
        }
    }
}
