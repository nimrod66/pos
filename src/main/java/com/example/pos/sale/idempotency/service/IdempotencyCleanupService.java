package com.example.pos.sale.idempotency.service;

import com.example.pos.sale.idempotency.model.IdempotencyKey;
import com.example.pos.sale.idempotency.repository.IdempotencyKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Retention job: completed checkout keys stop being useful after a few days
 * (clients retry within minutes) and would otherwise grow unbounded.
 */
@Slf4j
@Service
public class IdempotencyCleanupService {

    private final IdempotencyKeyRepository repo;

    public IdempotencyCleanupService(IdempotencyKeyRepository repo) {
        this.repo = repo;
    }

    @Scheduled(initialDelay = 300_000, fixedDelay = 3_600_000)
    @Transactional
    public void purgeExpiredKeys() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        long removed = repo.deleteByStatusAndCreatedAtBefore(
                IdempotencyKey.Status.COMPLETED, cutoff);
        if (removed > 0) {
            log.info("Purged {} completed idempotency keys older than {}", removed, cutoff);
        }
    }
}
