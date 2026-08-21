package com.example.pos.terminal.service;

import com.example.pos.terminal.dto.HeartbeatRequestDto;
import com.example.pos.terminal.model.Terminal;
import com.example.pos.terminal.model.TerminalHeartbeat;
import com.example.pos.terminal.model.TerminalStatus;
import com.example.pos.terminal.repository.TerminalHeartbeatRepository;
import com.example.pos.terminal.repository.TerminalRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TerminalHeartbeatService {

    private final TerminalHeartbeatRepository heartbeatRepository;
    private final TerminalRegistryRepository terminalRepository;

    public void receiveHeartbeat(HeartbeatRequestDto request) {
        Terminal terminal = terminalRepository.findByTerminalId(request.getTerminalId())
                .orElseThrow(() -> new com.example.pos.common.exception.ResourceNotFoundException(
                        "Terminal not found: " + request.getTerminalId()));

        terminal.setLastSeenAt(LocalDateTime.now());
        terminalRepository.save(terminal);

        TerminalHeartbeat heartbeat = TerminalHeartbeat.builder()
                .terminal(terminal)
                .batteryLevel(request.getBatteryLevel())
                .batteryCharging(request.getBatteryCharging())
                .networkType(request.getNetworkType())
                .signalStrength(request.getSignalStrength())
                .uptimeMinutes(request.getUptimeMinutes())
                .peripheralStatus(request.getPeripheralStatus())
                .additionalMetrics(request.getAdditionalMetrics())
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                .build();

        heartbeatRepository.save(heartbeat);
        log.debug("Heartbeat received from terminal {}", terminal.getTerminalId());
    }

    @Transactional(readOnly = true)
    public List<TerminalHeartbeat> getRecentHeartbeats(UUID terminalId) {
        return heartbeatRepository.findByTerminalIdOrderByTimestampDesc(terminalId)
                .stream()
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isTerminalOnline(String terminalId, int staleMinutes) {
        return terminalRepository.findByTerminalId(terminalId)
                .map(terminal -> {
                    if (terminal.getLastSeenAt() == null) return false;
                    return terminal.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(staleMinutes));
                })
                .orElse(false);
    }
}
