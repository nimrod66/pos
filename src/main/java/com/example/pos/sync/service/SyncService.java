package com.example.pos.sync.service;

import com.example.pos.sync.config.SyncProperties;
import com.example.pos.sync.config.TerminalConfig;
import com.example.pos.sync.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class SyncService {

    private final SyncEventRepository outboxRepo;
    private final ConnectivityService connectivity;
    private final TerminalConfig terminalConfig;
    private final SyncProperties syncProperties;
    private final RestTemplate restTemplate;

    public SyncService(SyncEventRepository outboxRepo,
                       ConnectivityService connectivity,
                       TerminalConfig terminalConfig,
                       SyncProperties syncProperties) {
        this.outboxRepo = outboxRepo;
        this.connectivity = connectivity;
        this.terminalConfig = terminalConfig;
        this.syncProperties = syncProperties;
        this.restTemplate = new RestTemplate();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void writeOutboxEvent(EventType eventType, String aggregateType, String aggregateId, String payload) {
        if (!syncProperties.isEnabled()) return;

        long existingCount = outboxRepo.countByAggregateTypeAndAggregateId(aggregateType, aggregateId);
        int nextVersion = (int) existingCount + 1;

        SyncEvent event = SyncEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .terminalId(terminalConfig.getTerminalId())
                .aggregateVersion(nextVersion)
                .sequenceNumber(nextVersion)
                .build();
        outboxRepo.save(event);
    }

    @Scheduled(fixedDelay = 2000)
    public void pushPendingEvents() {
        if (!syncProperties.isEnabled()) return;
        if (!connectivity.isOnline()) return;

        List<SyncEvent> pending = outboxRepo.findByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
                SyncEvent.SyncEventStatus.PENDING, LocalDateTime.now());

        for (SyncEvent event : pending) {
            tryPushEvent(event);
        }

        List<SyncEvent> failed = outboxRepo.findByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
                SyncEvent.SyncEventStatus.FAILED, LocalDateTime.now());

        for (SyncEvent event : failed) {
            tryPushEvent(event);
        }
    }

    private void tryPushEvent(SyncEvent event) {
        event.setStatus(SyncEvent.SyncEventStatus.SENDING);
        outboxRepo.save(event);

        try {
            String url = syncProperties.getCentralUrl() + "/api/v1/sync/push";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventId", event.getEventId());
            body.put("eventVersion", event.getEventVersion());
            body.put("aggregateType", event.getAggregateType());
            body.put("aggregateId", event.getAggregateId());
            body.put("aggregateVersion", event.getAggregateVersion());
            body.put("sequenceNumber", event.getSequenceNumber());
            body.put("eventType", event.getEventType().name());
            body.put("payload", event.getPayload());
            body.put("terminalId", event.getTerminalId());
            body.put("createdAt", event.getCreatedAt().toString());

            Map<String, Object> resp = restTemplate.postForObject(url, body, Map.class);

            if (resp != null) {
                String responseStatus = (String) resp.getOrDefault("status", "UNKNOWN");
                switch (responseStatus) {
                    case "ACCEPTED":
                    case "IDEMPOTENT_IGNORE":
                        event.markAcknowledged();
                        log.info("Sync event {} acknowledged: {}", event.getEventType(), event.getAggregateId());
                        break;
                    case "CONFLICT":
                        event.markDead("CONFLICT: " + resp.get("message"));
                        log.warn("Sync event {} conflicted: {}", event.getEventId(), resp.get("message"));
                        break;
                    case "REJECTED":
                        event.markFailed("REJECTED: " + resp.getOrDefault("message", "unknown"));
                        break;
                    default:
                        event.markFailed("UNKNOWN_RESPONSE: " + resp);
                }
            } else {
                event.markFailed("No response from central server");
            }
        } catch (Exception e) {
            if (event.getRetryCount() >= 10) {
                event.markDead("Max retries exceeded: " + e.getMessage());
                log.error("Sync event {} marked DEAD after {} retries", event.getEventId(), event.getRetryCount());
            } else {
                event.markFailed(e.getMessage());
                log.warn("Sync event {} failed (retry {}/{}): {}",
                        event.getEventId(), event.getRetryCount(), 10, e.getMessage());
            }
        }
        outboxRepo.save(event);
    }

    public Map<String, Object> pullCatalog(String since) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!connectivity.isOnline()) {
            result.put("success", false);
            result.put("reason", "Offline");
            return result;
        }
        try {
            String url = syncProperties.getCentralUrl() + "/api/v1/sync/pull/catalog"
                    + (since != null ? "?since=" + since : "");
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            result.put("success", true);
            result.put("data", resp);
        } catch (Exception e) {
            result.put("success", false);
            result.put("reason", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getQueueStatus() {
        long pending = outboxRepo.countByStatus(SyncEvent.SyncEventStatus.PENDING);
        long failed = outboxRepo.countByStatus(SyncEvent.SyncEventStatus.FAILED);
        long dead = outboxRepo.countByStatus(SyncEvent.SyncEventStatus.DEAD);
        long sent = outboxRepo.countByStatus(SyncEvent.SyncEventStatus.SENT);

        Optional<SyncEvent> lastAcknowledged = outboxRepo.findAll().stream()
                .filter(e -> e.getAcknowledgedAt() != null)
                .max(Comparator.comparing(SyncEvent::getAcknowledgedAt));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("pendingEvents", pending);
        status.put("retryQueue", failed);
        status.put("deadLetters", dead);
        status.put("successfullySynced", sent);
        status.put("currentlySyncing", false);
        status.put("lastSuccessfulSync", lastAcknowledged.map(e -> e.getAcknowledgedAt().toString()).orElse(null));
        status.put("estimatedSyncTime", estimateSyncTime(pending));
        status.put("connectivity", connectivity.getStatus());
        return status;
    }

    private String estimateSyncTime(long pending) {
        if (pending == 0) return "0s";
        long avgLatency = connectivity.getLastLatencyMs();
        if (avgLatency <= 0) avgLatency = 200;
        long totalMs = pending * avgLatency;
        if (totalMs < 1000) return totalMs + "ms";
        return (totalMs / 1000) + "s";
    }

    public List<SyncEvent> getDeadEvents() {
        return outboxRepo.findByStatusOrderByCreatedAtAsc(SyncEvent.SyncEventStatus.DEAD);
    }

    public SyncEvent retryDeadEvent(String eventId) {
        SyncEvent event = outboxRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getStatus() != SyncEvent.SyncEventStatus.DEAD) {
            throw new RuntimeException("Only DEAD events can be retried");
        }
        event.setStatus(SyncEvent.SyncEventStatus.PENDING);
        event.setRetryCount(0);
        event.setNextRetryAt(LocalDateTime.now());
        event.setLastError(null);
        event.setAggregateVersion(event.getAggregateVersion() + 1);
        return outboxRepo.save(event);
    }

    public void discardDeadEvent(String eventId) {
        SyncEvent event = outboxRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getStatus() != SyncEvent.SyncEventStatus.DEAD) {
            throw new RuntimeException("Only DEAD events can be discarded");
        }
        event.setStatus(SyncEvent.SyncEventStatus.IGNORED);
        outboxRepo.save(event);
    }

    public Object exportDeadEvent(String eventId) {
        SyncEvent event = outboxRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("eventId", event.getEventId());
        export.put("eventVersion", event.getEventVersion());
        export.put("aggregateType", event.getAggregateType());
        export.put("aggregateId", event.getAggregateId());
        export.put("aggregateVersion", event.getAggregateVersion());
        export.put("sequenceNumber", event.getSequenceNumber());
        export.put("eventType", event.getEventType().name());
        export.put("terminalId", event.getTerminalId());
        export.put("createdAt", event.getCreatedAt().toString());
        export.put("lastError", event.getLastError());
        export.put("retryCount", event.getRetryCount());
        export.put("payload", event.getPayload());
        return export;
    }

    public Map<String, Object> retryAllDeadEvents() {
        List<SyncEvent> dead = getDeadEvents();
        int retried = 0;
        int skipped = 0;
        for (SyncEvent event : dead) {
            try {
                retryDeadEvent(event.getEventId());
                retried++;
            } catch (Exception e) {
                skipped++;
                log.warn("Failed to retry dead event {}: {}", event.getEventId(), e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "COMPLETED");
        result.put("retried", retried);
        result.put("skipped", skipped);
        result.put("total", dead.size());
        return result;
    }

    public Map<String, Object> retryDeadEventsForTerminal(String terminalId) {
        List<SyncEvent> terminalDead = outboxRepo.findByTerminalIdAndStatus(terminalId, SyncEvent.SyncEventStatus.DEAD);
        int retried = 0;
        int skipped = 0;
        for (SyncEvent event : terminalDead) {
            try {
                retryDeadEvent(event.getEventId());
                retried++;
            } catch (Exception e) {
                skipped++;
                log.warn("Failed to retry dead event {} for terminal {}: {}",
                        event.getEventId(), terminalId, e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "COMPLETED");
        result.put("terminalId", terminalId);
        result.put("retried", retried);
        result.put("skipped", skipped);
        result.put("total", terminalDead.size());
        return result;
    }

    public List<Object> exportAllDeadEvents() {
        return getDeadEvents().stream()
                .map(e -> exportDeadEvent(e.getEventId()))
                .toList();
    }

    public Map<String, Object> receivePush(Map<String, Object> incoming) {
        String eventId = (String) incoming.get("eventId");
        String aggregateType = (String) incoming.get("aggregateType");
        String aggregateId = (String) incoming.get("aggregateId");

        boolean alreadyExists = outboxRepo.existsById(eventId);
        if (alreadyExists) {
            return Map.of("status", "IDEMPOTENT_IGNORE", "message", "Event already processed");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        ConflictPolicy policy = ConflictPolicy.forType(aggregateType);

        switch (policy) {
            case TERMINAL_WINS:
                SyncEvent event = SyncEvent.builder()
                        .eventId(eventId)
                        .aggregateType(aggregateType)
                        .aggregateId(aggregateId)
                        .eventType(EventType.valueOf((String) incoming.get("eventType")))
                        .payload((String) incoming.get("payload"))
                        .terminalId((String) incoming.get("terminalId"))
                        .aggregateVersion(incoming.get("aggregateVersion") instanceof Number
                                ? ((Number) incoming.get("aggregateVersion")).intValue() : 1)
                        .sequenceNumber(incoming.get("sequenceNumber") instanceof Number
                                ? ((Number) incoming.get("sequenceNumber")).intValue() : 1)
                        .status(SyncEvent.SyncEventStatus.SENT)
                        .build();
                outboxRepo.save(event);
                result.put("status", "ACCEPTED");
                result.put("message", "Terminal data accepted");
                break;

            case CENTRAL_WINS:
                result.put("status", "CONFLICT");
                result.put("message", "Central data is authoritative for " + aggregateType);
                break;

            case MOVEMENT_EVENTS:
                outboxRepo.save(SyncEvent.builder()
                        .eventId(eventId)
                        .aggregateType(aggregateType)
                        .aggregateId(aggregateId)
                        .eventType(EventType.valueOf((String) incoming.get("eventType")))
                        .payload((String) incoming.get("payload"))
                        .terminalId((String) incoming.get("terminalId"))
                        .aggregateVersion(incoming.get("aggregateVersion") instanceof Number
                                ? ((Number) incoming.get("aggregateVersion")).intValue() : 1)
                        .sequenceNumber(incoming.get("sequenceNumber") instanceof Number
                                ? ((Number) incoming.get("sequenceNumber")).intValue() : 1)
                        .status(SyncEvent.SyncEventStatus.SENT)
                        .build());
                result.put("status", "ACCEPTED");
                result.put("message", "Stock movement accepted — reconciled centrally");
                break;

            case VERSIONED:
                Integer incomingVersion = incoming.get("aggregateVersion") instanceof Number
                        ? ((Number) incoming.get("aggregateVersion")).intValue() : 1;
                int latestVersion = outboxRepo.findByAggregateTypeAndAggregateIdOrderByAggregateVersionDesc(
                                aggregateType, aggregateId).stream()
                        .findFirst()
                        .map(SyncEvent::getAggregateVersion)
                        .orElse(0);

                if (incomingVersion > latestVersion) {
                    outboxRepo.save(SyncEvent.builder()
                            .eventId(eventId)
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(EventType.valueOf((String) incoming.get("eventType")))
                            .payload((String) incoming.get("payload"))
                            .terminalId((String) incoming.get("terminalId"))
                            .aggregateVersion(incomingVersion)
                            .status(SyncEvent.SyncEventStatus.SENT)
                            .build());
                    result.put("status", "ACCEPTED");
                    result.put("message", "Newer version accepted — central reconciles");
                } else {
                    result.put("status", "IDEMPOTENT_IGNORE");
                    result.put("message", "Stale version " + incomingVersion + " <= latest " + latestVersion);
                }
                break;
        }
        return result;
    }

    public enum ConflictPolicy {
        TERMINAL_WINS,
        CENTRAL_WINS,
        MOVEMENT_EVENTS,
        VERSIONED;

        public static ConflictPolicy forType(String aggregateType) {
            return switch (aggregateType.toUpperCase()) {
                case "SALE", "PAYMENT", "SALE_RETURN" -> TERMINAL_WINS;
                case "STOCK" -> MOVEMENT_EVENTS;
                case "PRODUCT", "PRICE", "TAX", "PHARMACY", "BRANCH", "USER" -> CENTRAL_WINS;
                case "CUSTOMER" -> VERSIONED;
                default -> CENTRAL_WINS;
            };
        }
    }
}
