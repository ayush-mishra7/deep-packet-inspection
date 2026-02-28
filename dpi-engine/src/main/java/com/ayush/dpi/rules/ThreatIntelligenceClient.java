package com.ayush.dpi.rules;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A dummy client demonstrating how to integrate an external Threat Intelligence
 * API.
 * Protected by Resilience4j Circuit Breaker and Retry to prevent cascading
 * failures
 * if the external API goes down.
 */
@Slf4j
@Service
public class ThreatIntelligenceClient {

    private static final String THREAT_CLIENT = "threatIntelApi";

    /**
     * Look up reputation score for an IP.
     * Simulated network calls and random failures.
     */
    @Retry(name = THREAT_CLIENT, fallbackMethod = "lookupIpReputationFallback")
    @CircuitBreaker(name = THREAT_CLIENT, fallbackMethod = "lookupIpReputationFallback")
    public int lookupIpReputation(String ipAddress) {
        // Simulate network latency
        simulateLatencies();

        // Simulate 20% random failure rate
        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            throw new RuntimeException("Threat Intelligence API Timeout / Unreachable");
        }

        // Return a dummy score (0 to 100, > 75 is malicious)
        return ThreadLocalRandom.current().nextInt(100);
    }

    /**
     * Fallback method if the circuit breaker is OPEN or retries fail.
     * Fail-open: Assume IP is safe (score 0) if threat API is down,
     * so we don't block legitimate traffic by accident.
     */
    @SuppressWarnings("unused")
    public int lookupIpReputationFallback(String ipAddress, Throwable t) {
        log.warn("Threat API unreachable for IP {}. Fallback triggered: {}", ipAddress, t.getMessage());
        return 0; // Safe baseline
    }

    private void simulateLatencies() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
