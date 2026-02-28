package com.ayush.dpi;

import com.ayush.dpi.config.DpiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point for the DPI Engine application.
 * <p>
 * A high-performance Deep Packet Inspection engine capable of processing
 * network packets from PCAP files or live interfaces, extracting metadata,
 * building logical connections, applying dynamic rules, and generating
 * real-time statistics.
 * </p>
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan("com.ayush.dpi.config")
public class DpiEngineApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(DpiEngineApplication.class, args);

        DpiProperties props = ctx.getBean(DpiProperties.class);
        log.info("════════════════════════════════════════");
        log.info("  DPI Engine started successfully");
        log.info("  Mode    : {}", props.getMode().toUpperCase());
        log.info("  Port    : 8080");
        log.info("  Version : {}", props.getVersion());
        log.info("════════════════════════════════════════");
    }
}
