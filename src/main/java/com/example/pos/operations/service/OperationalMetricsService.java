package com.example.pos.operations.service;

import com.example.pos.core.branch.model.Branch;
import com.example.pos.core.pharmacy.model.Pharmacy;
import com.example.pos.operations.model.OperationalMetricEvent;
import com.example.pos.operations.repository.OperationalMetricEventRepository;
import com.example.pos.security.auth.AuthenticatedUserContext;
import com.example.pos.user.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OperationalMetricsService {

    private final OperationalMetricEventRepository repository;
    private final AuthenticatedUserContext current;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationalMetricEvent.EventType type,
                       OperationalMetricEvent.EventStatus status,
                       String reasonCode,
                       String source,
                       String terminalId,
                       UUID resourceId,
                       String idempotencyKey,
                       Long latencyMs,
                       String details) {
        User user = null;
        Branch branch = null;
        Pharmacy pharmacy = null;
        try {
            user = current.user();
            branch = current.branch();
            pharmacy = current.pharmacy();
        } catch (Exception ignored) {
            // Some operational events happen before or outside an authenticated session.
        }
        repository.save(OperationalMetricEvent.builder()
                .eventType(type)
                .status(status)
                .reasonCode(truncate(reasonCode, 96))
                .source(truncate(source, 96))
                .terminalId(truncate(terminalId, 96))
                .resourceId(resourceId)
                .idempotencyKey(truncate(idempotencyKey, 160))
                .latencyMs(latencyMs)
                .details(truncate(details, 4000))
                .user(user)
                .branch(branch)
                .pharmacy(pharmacy)
                .build());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(LocalDateTime from, LocalDateTime to, UUID branchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("branchId", branchId);

        Map<String, Map<String, Long>> byType = new LinkedHashMap<>();
        for (Object[] row : repository.countByTypeAndStatus(from, to, branchId)) {
            String type = String.valueOf(row[0]);
            String status = String.valueOf(row[1]);
            Long count = (Long) row[2];
            byType.computeIfAbsent(type, ignored -> new LinkedHashMap<>()).put(status, count);
        }
        result.put("events", byType);
        result.put("checkout", checkoutSummary(byType));
        result.put("checkoutFailureReasons", reasons(OperationalMetricEvent.EventType.CHECKOUT,
                OperationalMetricEvent.EventStatus.FAILED, from, to, branchId));
        result.put("paymentFailureReasons", reasons(OperationalMetricEvent.EventType.PAYMENT,
                OperationalMetricEvent.EventStatus.FAILED, from, to, branchId));
        result.put("hardwareFailureReasons", reasons(OperationalMetricEvent.EventType.HARDWARE,
                OperationalMetricEvent.EventStatus.FAILED, from, to, branchId));
        return result;
    }

    private Map<String, Object> checkoutSummary(Map<String, Map<String, Long>> byType) {
        Map<String, Long> checkout = byType.getOrDefault("CHECKOUT", Map.of());
        long attempted = checkout.getOrDefault("ATTEMPTED", 0L);
        long success = checkout.getOrDefault("SUCCESS", 0L);
        long failed = checkout.getOrDefault("FAILED", 0L);
        long denominator = Math.max(attempted, success + failed);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", attempted);
        result.put("success", success);
        result.put("failed", failed);
        result.put("successRatePercent", denominator == 0 ? null : Math.round(success * 10000.0 / denominator) / 100.0);
        return result;
    }

    private Map<String, Long> reasons(OperationalMetricEvent.EventType type,
                                      OperationalMetricEvent.EventStatus status,
                                      LocalDateTime from,
                                      LocalDateTime to,
                                      UUID branchId) {
        Map<String, Long> result = new LinkedHashMap<>();
        List<Object[]> rows = repository.countReasons(type, status, from, to, branchId);
        for (Object[] row : rows) {
            result.put(row[0] == null ? "UNKNOWN" : String.valueOf(row[0]), (Long) row[1]);
        }
        return result;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
