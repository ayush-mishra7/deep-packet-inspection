package com.ayush.dpi.stats;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Central thread-safe statistics aggregator.
 * <p>
 * Uses {@link LongAdder} and {@link ConcurrentHashMap} to accept highly
 * concurrent flush updates from worker threads with minimal contention.
 * Integrates directly with Spring Boot Actuator/Micrometer.
 * </p>
 */
@Slf4j
@Service
public class StatsService {

    private final LongAdder totalPacketsProcessed = new LongAdder();
    private final LongAdder totalBytesProcessed = new LongAdder();
    private final LongAdder tcpPackets = new LongAdder();
    private final LongAdder udpPackets = new LongAdder();
    private final LongAdder allowedPackets = new LongAdder();
    private final LongAdder blockedPackets = new LongAdder();
    private final LongAdder throttledPackets = new LongAdder();

    private final ConcurrentHashMap<String, LongAdder> domainFrequency = new ConcurrentHashMap<>();

    public StatsService(MeterRegistry registry) {
        // Register gauges with Micrometer for Actuator endpoints (/actuator/metrics)
        registry.gauge("dpi.packets.total", totalPacketsProcessed);
        registry.gauge("dpi.bytes.total", totalBytesProcessed);
        registry.gauge("dpi.packets.protocol", Tags.of("protocol", "tcp"), tcpPackets);
        registry.gauge("dpi.packets.protocol", Tags.of("protocol", "udp"), udpPackets);
        registry.gauge("dpi.decisions", Tags.of("decision", "allow"), allowedPackets);
        registry.gauge("dpi.decisions", Tags.of("decision", "block"), blockedPackets);
        registry.gauge("dpi.decisions", Tags.of("decision", "throttle"), throttledPackets);
    }

    /**
     * Accept a periodic flush from a worker thread.
     *
     * @param localStats the local stats delta to add to global totals
     */
    public void recordMetrics(WorkerLocalStats localStats) {
        if (localStats.getPacketsProcessed() == 0)
            return;

        totalPacketsProcessed.add(localStats.getPacketsProcessed());
        totalBytesProcessed.add(localStats.getBytesProcessed());
        tcpPackets.add(localStats.getTcpPackets());
        udpPackets.add(localStats.getUdpPackets());
        allowedPackets.add(localStats.getAllowedPackets());
        blockedPackets.add(localStats.getBlockedPackets());
        throttledPackets.add(localStats.getThrottledPackets());

        for (Map.Entry<String, Long> entry : localStats.getDomainFrequency().entrySet()) {
            domainFrequency.computeIfAbsent(entry.getKey(), k -> new LongAdder())
                    .add(entry.getValue());
        }
    }

    public long getTotalPackets() {
        return totalPacketsProcessed.sum();
    }

    public long getTotalBytes() {
        return totalBytesProcessed.sum();
    }

    public long getTcpPackets() {
        return tcpPackets.sum();
    }

    public long getUdpPackets() {
        return udpPackets.sum();
    }

    public long getAllowedPackets() {
        return allowedPackets.sum();
    }

    public long getBlockedPackets() {
        return blockedPackets.sum();
    }

    public long getThrottledPackets() {
        return throttledPackets.sum();
    }

    public Map<String, LongAdder> getDomainFrequency() {
        return domainFrequency;
    }

    /**
     * Periodic health log. Configurable via dpi.stats.log-interval in
     * application.yml.
     */
    @Scheduled(fixedRateString = "${dpi.stats.log-interval:10000}")
    public void logSummary() {
        long total = totalPacketsProcessed.sum();
        if (total == 0)
            return; // Silent if no traffic

        log.info("--- DPI Pipeline Stats ---");
        log.info("Packets: {} | Bytes: {}", total, totalBytesProcessed.sum());
        log.info("Decisions: ALLOW({}) BLOCK({}) THROTTLE({})",
                allowedPackets.sum(), blockedPackets.sum(), throttledPackets.sum());
        log.info("Protocols: TCP({}) UDP({})", tcpPackets.sum(), udpPackets.sum());
        log.info("--------------------------");
    }
}
