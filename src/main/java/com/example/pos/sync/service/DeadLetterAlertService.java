package com.example.pos.sync.service;

import com.example.pos.notification.model.Notification;
import com.example.pos.notification.service.NotificationService;
import com.example.pos.sync.event.SyncEvent;
import com.example.pos.sync.event.SyncEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeadLetterAlertService {

    private final SyncEventRepository outboxRepo;
    private final NotificationService notificationService;
    private final Set<String> previouslyAlertedEventIds = ConcurrentHashMap.newKeySet();

    private static final int DEAD_THRESHOLD = 5;
    private static final int TERMINAL_DEAD_THRESHOLD = 3;

    public DeadLetterAlertService(SyncEventRepository outboxRepo,
                                   NotificationService notificationService) {
        this.outboxRepo = outboxRepo;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 30_000)
    public void scanDeadLetters() {
        List<SyncEvent> deadEvents = outboxRepo.findByStatusOrderByCreatedAtAsc(SyncEvent.SyncEventStatus.DEAD);

        if (deadEvents.isEmpty()) {
            return;
        }

        Map<String, List<SyncEvent>> byTerminal = deadEvents.stream()
                .collect(Collectors.groupingBy(SyncEvent::getTerminalId));

        List<SyncEvent> newDead = deadEvents.stream()
                .filter(e -> !previouslyAlertedEventIds.contains(e.getEventId()))
                .toList();

        if (!newDead.isEmpty()) {
            log.warn("DEAD LETTER ALERT: {} new dead events (total: {}) across {} terminals",
                    newDead.size(), deadEvents.size(), byTerminal.size());
            createSystemAlert(newDead.size(), deadEvents.size(), byTerminal);
            newDead.forEach(e -> previouslyAlertedEventIds.add(e.getEventId()));
        }

        Map<String, Long> terminalDeadCounts = new LinkedHashMap<>();
        byTerminal.forEach((tid, list) -> terminalDeadCounts.put(tid, (long) list.size()));

        Set<String> overloadedTerminals = terminalDeadCounts.entrySet().stream()
                .filter(e -> e.getValue() >= TERMINAL_DEAD_THRESHOLD)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!overloadedTerminals.isEmpty()) {
            log.warn("TERMINAL HEALTH: {} terminals have {} or more dead events: {}",
                    overloadedTerminals.size(), TERMINAL_DEAD_THRESHOLD, overloadedTerminals);
            createTerminalAlert(overloadedTerminals, terminalDeadCounts, deadEvents.size());
        }

        int previousSize = previouslyAlertedEventIds.size();
        if (previousSize > 10_000) {
            previouslyAlertedEventIds.clear();
            deadEvents.forEach(e -> previouslyAlertedEventIds.add(e.getEventId()));
            log.info("Dead letter alert cache recycled ({} entries)", previousSize);
        }
    }

    private void createSystemAlert(int newCount, int totalCount,
                                    Map<String, List<SyncEvent>> byTerminal) {
        SyncEvent sample = byTerminal.values().iterator().next().get(0);
        String message = String.format(
                "%d new sync events went DEAD (total: %d). Affected terminals: %s. Sample error: %s",
                newCount, totalCount,
                byTerminal.keySet().stream().limit(5).collect(Collectors.joining(", ")),
                truncate(sample.getLastError(), 200));
        createNotification("SYSTEM_ALERT", "Sync Dead Letter Alert", message, sample);
    }

    private void createTerminalAlert(Set<String> terminalIds,
                                      Map<String, Long> counts, int totalDead) {
        SyncEvent sample = outboxRepo.findByStatusOrderByCreatedAtAsc(SyncEvent.SyncEventStatus.DEAD).get(0);
        String terminals = terminalIds.stream()
                .map(t -> t + " (" + counts.get(t) + " dead)")
                .collect(Collectors.joining(", "));
        String message = String.format(
                "Terminals with excessive dead letters: %s. Review dead letter dashboard immediately.",
                terminals);
        createNotification("SYSTEM_ALERT", "Terminal Dead Letter Overload", message, sample);
    }

    private void createNotification(String type, String title, String message, SyncEvent sample) {
        try {
            notificationService.create(title, message, Notification.Type.valueOf(type),
                    null, null, "DEAD_LETTER_ALERT");
        } catch (Exception e) {
            log.error("Failed to create dead letter notification: {}", e.getMessage());
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "none";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    public Map<String, Object> getDeadLetterStats() {
        long dead = outboxRepo.countByStatus(SyncEvent.SyncEventStatus.DEAD);

        List<SyncEvent> deadEvents = outboxRepo.findByStatusOrderByCreatedAtAsc(SyncEvent.SyncEventStatus.DEAD);
        Map<String, Long> byTerminal = deadEvents.stream()
                .collect(Collectors.groupingBy(SyncEvent::getTerminalId, Collectors.counting()));

        Map<String, Long> byType = deadEvents.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventType().name(), Collectors.counting()));

        List<Map<String, Object>> unhealthyTerminals = byTerminal.entrySet().stream()
                .filter(e -> e.getValue() >= TERMINAL_DEAD_THRESHOLD)
                .map(e -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("terminalId", e.getKey());
                    info.put("deadCount", e.getValue());
                    info.put("lastDeadAt", deadEvents.stream()
                            .filter(ev -> e.getKey().equals(ev.getTerminalId()))
                            .map(SyncEvent::getCreatedAt)
                            .max(Comparator.naturalOrder())
                            .map(d -> d.toString()).orElse(null));
                    return info;
                }).toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDead", dead);
        stats.put("byTerminal", byTerminal);
        stats.put("byEventType", byType);
        stats.put("unhealthyTerminals", unhealthyTerminals);
        stats.put("deadThreshold", TERMINAL_DEAD_THRESHOLD);
        return stats;
    }
}
