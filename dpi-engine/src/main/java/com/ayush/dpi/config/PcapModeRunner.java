package com.ayush.dpi.config;

import com.ayush.dpi.capture.PacketIngestionService;
import com.ayush.dpi.capture.PcapFilePacketSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Executes PCAP file processing when the application starts in {@code pcap}
 * mode.
 * <p>
 * Reads all packets from the configured file, forwards them through the
 * ingestion pipeline, and then initiates a graceful shutdown.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PcapModeRunner implements ApplicationRunner {

    private final DpiProperties properties;
    private final PacketIngestionService ingestionService;
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

        try {
            PcapFilePacketSource source = new PcapFilePacketSource(pcapPath);

            // Stub downstream handler — logs receipt at TRACE level.
            // Will be replaced by the parser layer in Phase 3.
            ingestionService.ingest(source, rawPacket -> log.trace("Received packet #{} ({} bytes)",
                    rawPacket.getSequenceNumber(), rawPacket.getLength()));

            log.info("PCAP processing completed successfully");
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
