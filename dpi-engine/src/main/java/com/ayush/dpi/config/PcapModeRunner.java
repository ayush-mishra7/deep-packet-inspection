package com.ayush.dpi.config;

import com.ayush.dpi.capture.PacketIngestionService;
import com.ayush.dpi.capture.PcapFilePacketSource;
import com.ayush.dpi.loadbalancer.LoadBalancerService;
import com.ayush.dpi.parser.PacketParserService;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.worker.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Executes PCAP file processing when the application starts in {@code pcap}
 * mode.
 * <p>
 * Pipeline: Capture → Parse → LoadBalancer → Workers (connection tracking).
 * After ingestion completes, workers are shut down and per-worker connection
 * summaries are logged.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PcapModeRunner implements ApplicationRunner {

    private final DpiProperties properties;
    private final PacketIngestionService ingestionService;
    private final PacketParserService parserService;
    private final LoadBalancerService loadBalancerService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        if (!"pcap".equalsIgnoreCase(properties.getMode())) {
            log.info("Running in SERVER mode — PCAP runner skipped");
            return;
        }

        String pcapPath = properties.getCapture().getPcapFilePath();
        if (pcapPath == null || pcapPath.isBlank()) {
            log.error("PCAP mode requires 'dpi.capture.pcap-file-path' to be set");
            initiateShutdown(1);
            return;
        }

        log.info("Running in PCAP mode — processing file: {}", pcapPath);

        AtomicLong parsedCount = new AtomicLong(0);
        AtomicLong skippedCount = new AtomicLong(0);

        try {
            // Initialize the multi-threaded worker pool
            loadBalancerService.init();

            PcapFilePacketSource source = new PcapFilePacketSource(pcapPath);

            ingestionService.ingest(source, rawPacket -> {
                Optional<ParsedPacket> result = parserService.parse(rawPacket);

                if (result.isPresent()) {
                    ParsedPacket pkt = result.get();
                    parsedCount.incrementAndGet();
                    loadBalancerService.dispatch(pkt);
                } else {
                    skippedCount.incrementAndGet();
                }
            });

            // Allow workers to drain their queues
            Thread.sleep(500);

            // Shutdown workers and print summary
            loadBalancerService.shutdown();

            log.info("═══════════════════════════════════════");
            log.info("  Pipeline Summary");
            log.info("  Parsed     : {}", parsedCount.get());
            log.info("  Skipped    : {}", skippedCount.get());
            log.info("  Dispatched : {}", loadBalancerService.getDispatchedCount());
            log.info("  Dropped    : {}", loadBalancerService.getDropCount());
            log.info("═══════════════════════════════════════");

        } catch (Exception e) {
            log.error("PCAP processing failed: {}", e.getMessage(), e);
        } finally {
            initiateShutdown(0);
        }
    }

    private void initiateShutdown(int exitCode) {
        log.info("Initiating graceful shutdown (exit code: {})", exitCode);
        SpringApplication.exit(applicationContext, () -> exitCode);
    }
}
