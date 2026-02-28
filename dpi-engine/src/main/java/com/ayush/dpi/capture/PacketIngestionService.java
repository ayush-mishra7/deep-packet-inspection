package com.ayush.dpi.capture;

import com.ayush.dpi.config.DpiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates packet ingestion from a {@link PacketSource} and forwards
 * each raw packet downstream through a {@link PacketHandler} pipeline.
 * <p>
 * Provides configurable batch-logging of progress and logs a summary
 * (total packets, elapsed time, throughput) when ingestion completes.
 * </p>
 */
@Slf4j
@Service
public class PacketIngestionService {

    private final DpiProperties properties;
    private final AtomicLong totalPacketsProcessed = new AtomicLong(0);

    public PacketIngestionService(DpiProperties properties) {
        this.properties = properties;
    }

    /**
     * Run the full ingestion pipeline: read from the given source and forward
     * each packet to the downstream handler.
     *
     * @param source     the packet source to read from
     * @param downstream the handler to forward packets to (e.g., parser layer)
     */
    public void ingest(PacketSource source, PacketHandler downstream) {
        int batchInterval = properties.getCapture().getBatchLogInterval();
        totalPacketsProcessed.set(0);

        log.info("Starting packet ingestion (batch log every {} packets)", batchInterval);
        Instant start = Instant.now();

        PacketHandler instrumentedHandler = (rawPacket) -> {
            long count = totalPacketsProcessed.incrementAndGet();

            // Forward to downstream handler (parser layer in future phases)
            downstream.handle(rawPacket);

            // Batch progress logging
            if (count % batchInterval == 0) {
                Duration elapsed = Duration.between(start, Instant.now());
                double pps = count / Math.max(elapsed.toMillis() / 1000.0, 0.001);
                log.info("Progress: {} packets processed ({} elapsed, {} pkt/s)",
                        count, formatDuration(elapsed), String.format("%.0f", pps));
            }
        };

        source.start(instrumentedHandler);

        // Final summary
        long total = totalPacketsProcessed.get();
        Duration elapsed = Duration.between(start, Instant.now());
        double pps = total / Math.max(elapsed.toMillis() / 1000.0, 0.001);

        log.info("═══════════════════════════════════════");
        log.info("  Ingestion Complete");
        log.info("  Total packets : {}", total);
        log.info("  Elapsed time  : {}", formatDuration(elapsed));
        log.info("  Throughput    : {} packets/sec", String.format("%.0f", pps));
        log.info("═══════════════════════════════════════");
    }

    /**
     * @return total packets processed across all ingestion runs
     */
    public long getTotalPacketsProcessed() {
        return totalPacketsProcessed.get();
    }

    private String formatDuration(Duration d) {
        long seconds = d.getSeconds();
        long millis = d.toMillisPart();
        if (seconds < 60) {
            return String.format("%d.%03ds", seconds, millis);
        }
        return String.format("%dm %d.%03ds", seconds / 60, seconds % 60, millis);
    }
}
