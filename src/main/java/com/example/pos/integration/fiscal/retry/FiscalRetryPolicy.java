package com.example.pos.integration.fiscal.retry;

public record FiscalRetryPolicy(
        int maxAttempts,
        long initialBackoffMs,
        long maxBackoffMs
) {
    public long backoffForAttempt(int attempt) {
        if (attempt <= 0) return initialBackoffMs;
        long backoff = initialBackoffMs * (1L << Math.min(attempt, 10));
        return Math.min(backoff, maxBackoffMs);
    }
}
