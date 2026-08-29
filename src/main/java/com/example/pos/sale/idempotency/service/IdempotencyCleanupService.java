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
 *
 * Also recovers orphaned IN_PROGRESS keys from crashes. A checkout transaction
 * should complete within seconds. If an IN_PROGRESS key has been stale for
 * more than 1 hour, the original transaction either committed (and the key
 * should be COMPLETED) or rolled back (and the key can be safely deleted
 * to allow retry).
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

    /**
     * Recover orphaned IN_PROGRESS keys from crashes. A checkout should
     * complete within seconds. Keys stale for >1 hour indicate the original
     * transaction either committed (key should be COMPLETED) or rolled back
     * (key can be deleted to allow retry).
     *
     * Runs every 5 minutes after an initial 2-minute delay.
     */
    @Scheduled(initialDelay = 120_000, fixedDelay = 300_000)
    @Transactional
    public void recoverStaleInProgressKeys() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(1);
        long recovered = repo.deleteByStatusAndCreatedAtBefore(
                IdempotencyKey.Status.IN_PROGRESS, staleThreshold);
        if (recovered > 0) {
            log.warn("Recovered {} stale IN_PROGRESS idempotency keys older than {}",
                    recovered, staleThreshold);
        }
    }
}
