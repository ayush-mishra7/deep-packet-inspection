package com.ayush.dpi.config;

import com.ayush.dpi.benchmark.SyntheticTrafficSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runner that executes the benchmarking synthetic simulator if enabled via
 * properties.
 * Exits the application when the benchmark successfully finishes.
 */
@Slf4j
@Component
@Order(3) // Ensure LoadBalancer and other Beans are fully initialized first
@RequiredArgsConstructor
public class BenchmarkRunner implements ApplicationRunner {

    private final DpiProperties properties;
    private final SyntheticTrafficSimulator simulator;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if ("benchmark".equalsIgnoreCase(properties.getMode())) {
            log.info("Benchmarking mode enabled. Initiating Synthetic Traffic Simulator...");

            // Allow JVM & Spring context to settle
            Thread.sleep(1000);

            // In a real scenario, these could be configurable. For now, we inject 1 Million
            // packets.
            int totalPackets = 1_000_000;
            int batchSize = 1000;

            simulator.runBenchmark(totalPackets, batchSize);

            log.info("Benchmark complete. Exiting application.");
            System.exit(0);
        }
    }
}
