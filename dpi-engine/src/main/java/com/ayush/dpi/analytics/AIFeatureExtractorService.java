package com.ayush.dpi.analytics;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.loadbalancer.LoadBalancerService;
import com.ayush.dpi.worker.WorkerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Periodically extracts connection metadata into structured JSON formats
 * suitable for ingestion by external Machine Learning pipelines
 * (e.g., Python microservices or ELK stack).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIFeatureExtractorService {

    private final LoadBalancerService loadBalancerService;
    private final ObjectMapper objectMapper;

    /**
     * Extracts features every 30 seconds for all active connections.
     * Generates a JSON line per connection containing:
     * - packet_frequency (pps)
     * - bytes_per_second (bps)
     * - session_duration_ms
     * - burst_pattern_indicator (heuristic)
     */
    @Scheduled(fixedRate = 30000)
    public void extractAIModelFeatures() {
        if (!loadBalancerService.isInitialized() || loadBalancerService.getWorkers() == null) {
            return;
        }

        Instant now = Instant.now();
        int exported = 0;

        // Iterate over all worker thread connection maps
        for (WorkerService worker : loadBalancerService.getWorkers()) {
            for (Connection conn : worker.getConnections().values()) {

                // Only extract features for connections that had recent activity
                Duration duration = Duration.between(conn.getFirstSeen(), now);
                long durationMs = duration.toMillis();

                if (durationMs < 1000)
                    continue; // Skip too new connections

                double durationSec = durationMs / 1000.0;
                double packetsPerSecond = conn.getPacketCount() / durationSec;
                double bytesPerSecond = conn.getBytesTransferred() / durationSec;

                // Simple burst indicator: high pps relative to total duration
                double burstIndicator = (packetsPerSecond > 100 && durationSec < 5.0) ? 1.0 : 0.0;

                Map<String, Object> features = new HashMap<>();
                features.put("src_ip", conn.getSrcIp());
                features.put("dest_ip", conn.getDestIp());
                features.put("dest_port", conn.getDestPort());
                features.put("protocol", conn.getProtocol().name());
                features.put("session_duration_ms", durationMs);
                features.put("packet_frequency_pps", packetsPerSecond);
                features.put("bytes_per_second_bps", bytesPerSecond);
                features.put("burst_indicator", burstIndicator);
                features.put("last_decision", conn.getLastDecision().name());

                try {
                    String jsonFeatureLine = objectMapper.writeValueAsString(features);
                    // In a real system, you'd send this to Kafka. For now, we log to a dedicated ML
                    // logger.
                    log.trace("AI_FEATURE_EXTRACT: {}", jsonFeatureLine);
                    exported++;
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize ML feature set for connection", e);
                }
            }
        }

        if (exported > 0) {
            log.debug("AIFeatureExtractor exported {} connection feature vectors", exported);
        }
    }
}
