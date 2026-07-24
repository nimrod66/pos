package com.example.pos.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Value("${pos.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${pos.rate-limit.requests-per-second:20}")
    private double requestsPerSecond;

    @Value("${pos.rate-limit.auth-requests-per-second:5}")
    private double authRequestsPerSecond;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        double rate = resolveRate(request);
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(rate));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key={} path={}", key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Try again later.\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/h2-console")
                || path.startsWith("/ws")
                || path.startsWith("/static")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/h2-console")
                || path.startsWith("/ws")
                || path.startsWith("/static")
                || path.startsWith("/api/payments/") && (path.endsWith("/callback") || path.endsWith("/webhook"))
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private String resolveKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return "jwt:" + auth.substring(7);
        }
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return "apikey:" + apiKey;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return "ip:" + ip;
    }

    private double resolveRate(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/auth/")) {
            return authRequestsPerSecond;
        }
        return requestsPerSecond;
    }

    private static class TokenBucket {
        private final double rate;
        private final AtomicLong tokens;
        private final AtomicLong lastRefill;

        TokenBucket(double rate) {
            this.rate = rate;
            this.tokens = new AtomicLong((long) rate);
            this.lastRefill = new AtomicLong(System.nanoTime());
        }

        boolean tryConsume() {
            refill();
            long current = tokens.get();
            while (current > 0) {
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = tokens.get();
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long last = lastRefill.get();
            long elapsed = now - last;
            long newTokens = (long) (elapsed * rate / 1_000_000_000L);
            if (newTokens > 0) {
                if (lastRefill.compareAndSet(last, now)) {
                    tokens.addAndGet(newTokens);
                }
            }
        }
    }
}
