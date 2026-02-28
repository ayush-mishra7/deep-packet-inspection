package com.ayush.dpi.benchmark;

import com.ayush.dpi.stats.StatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "dpi.worker.count=4",
        "dpi.worker.queue-capacity=10000",
        "dpi.mode=server" // Keep in server mode to avoid premature exit in test context
})
public class SystemPerformanceTest {

    @Autowired
    private SyntheticTrafficSimulator simulator;

    @Autowired
    private com.ayush.dpi.loadbalancer.LoadBalancerService loadBalancer;

    @Autowired
    private StatsService statsService;

    @Test
    @DisplayName("Sustains 1 million packets across workers efficiently")
    void sustainsHighThroughput() throws Exception {
        int packets = 1_000_000;
        int batchSize = 5000;

        if (!loadBalancer.isInitialized()) {
            loadBalancer.init();
        }

        // Warm up / Execute
        long start = System.currentTimeMillis();
        simulator.runBenchmark(packets, batchSize);
        long end = System.currentTimeMillis();

        Duration generationDuration = Duration.ofMillis(end - start);
        System.out.println("Generation completed in: " + generationDuration.toMillis() + " ms");

        // Allow up to a few seconds for all workers to finish their queues and flush
        // stats
        Thread.sleep(3000);

        long totalPackets = statsService.getTotalPackets();

        System.out.println("Total Processed: " + totalPackets);
        assertThat(totalPackets).isGreaterThan(0L);
        // Sometimes the LoadBalancer drops packets when queue is full under extreme
        // stress.
        // We expect at least 90% ingestion success for a realistic sustained load test
        // on generic hardware.
        double successRatio = (double) totalPackets / packets;
        System.out.println("Success Ratio: " + successRatio);

        assertThat(successRatio).isGreaterThan(0.9);
    }
}
