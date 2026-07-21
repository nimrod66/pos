package com.example.pos.compliance.certification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class TestScenarioRunner {

    private static final Logger log = LoggerFactory.getLogger(TestScenarioRunner.class);

    public Map<String, Object> runScenario(String scenarioName, Supplier<Map<String, Object>> test) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", scenarioName);
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> details = test.get();
            result.put("status", "PASS");
            result.put("details", details);
            log.info("[CERT] {} - PASS ({}ms)", scenarioName, System.currentTimeMillis() - start);
        } catch (Exception e) {
            result.put("status", "FAIL");
            result.put("error", e.getMessage());
            log.error("[CERT] {} - FAIL: {}", scenarioName, e.getMessage());
        }

        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }
}
