package com.example.pos.sync.service;

import com.example.pos.sync.config.SyncProperties;
import com.example.pos.sync.config.TerminalConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class ConnectivityService {

    private final SyncProperties syncProperties;
    private final TerminalConfig terminalConfig;
    private final RestTemplate restTemplate;

    @Getter
    private volatile boolean online;

    @Getter
    private volatile LocalDateTime lastOnlineTime;

    @Getter
    private volatile LocalDateTime lastOfflineTime;

    @Getter
    private volatile long lastLatencyMs;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicLong heartbeatCount = new AtomicLong(0);

    public ConnectivityService(SyncProperties syncProperties, TerminalConfig terminalConfig) {
        this.syncProperties = syncProperties;
        this.terminalConfig = terminalConfig;
        this.restTemplate = new RestTemplate();
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate.setRequestFactory(factory);
        this.online = true;
    }

    public boolean checkConnectivity() {
        if (terminalConfig.isOffline()) return true;

        long start = System.currentTimeMillis();
        try {
            String url = syncProperties.getCentralUrl() + "/api/sync/health";
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            long latency = System.currentTimeMillis() - start;
            lastLatencyMs = latency;
            totalLatencyMs.addAndGet(latency);
            heartbeatCount.incrementAndGet();

            boolean isOnline = resp != null && Boolean.TRUE.equals(resp.get("success"));
            if (isOnline) {
                consecutiveFailures.set(0);
                lastOnlineTime = LocalDateTime.now();
                if (!online) {
                    log.info("CONNECTION RESTORED — online ({}ms latency)", latency);
                }
            } else {
                fail();
            }
            online = isOnline;
            return online;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            lastLatencyMs = latency;
            heartbeatCount.incrementAndGet();
            fail();
            return false;
        }
    }

    private void fail() {
        int failures = consecutiveFailures.incrementAndGet();
        if (online) {
            log.warn("CONNECTION LOST — {} consecutive failures", failures);
        }
        lastOfflineTime = LocalDateTime.now();
        online = false;
    }

    @Scheduled(fixedDelay = 15000)
    public void heartbeat() {
        checkConnectivity();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("mode", terminalConfig.isOffline() ? "OFFLINE" : "ONLINE");
        status.put("connected", online);
        status.put("terminalId", terminalConfig.getTerminalId());
        status.put("centralUrl", syncProperties.getCentralUrl());
        status.put("lastOnlineTime", lastOnlineTime);
        status.put("lastOfflineTime", lastOfflineTime);
        status.put("lastCheckTime", LocalDateTime.now());
        status.put("lastLatencyMs", lastLatencyMs);
        status.put("avgLatencyMs", heartbeatCount.get() > 0
                ? totalLatencyMs.get() / heartbeatCount.get() : 0);
        status.put("consecutiveFailures", consecutiveFailures.get());
        status.put("heartbeatCount", heartbeatCount.get());
        return status;
    }
}
