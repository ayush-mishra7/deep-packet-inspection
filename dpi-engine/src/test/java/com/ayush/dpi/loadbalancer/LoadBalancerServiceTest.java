package com.ayush.dpi.loadbalancer;

import com.ayush.dpi.config.DpiProperties;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import com.ayush.dpi.worker.WorkerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LoadBalancerService}.
 */
class LoadBalancerServiceTest {

    private DpiProperties properties;
    private LoadBalancerService loadBalancer;

    @BeforeEach
    void setUp() {
        properties = new DpiProperties();
        properties.getWorker().setCount(2);
        properties.getWorker().setQueueCapacity(100);
        loadBalancer = new LoadBalancerService(properties, new com.ayush.dpi.rules.RuleRegistry(),
                new com.ayush.dpi.stats.StatsService(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        loadBalancer.init();
    }

    @AfterEach
    void tearDown() {
        if (loadBalancer.isInitialized()) {
            loadBalancer.shutdown();
        }
    }

    @Test
    @DisplayName("Same-connection packets are dispatched to the same worker")
    void sameConnectionGoesToSameWorker() throws Exception {
        ParsedPacket p1 = buildPacket("10.0.0.1", "10.0.0.2", 5000, 80, ProtocolType.TCP, 1);
        ParsedPacket p2 = buildPacket("10.0.0.1", "10.0.0.2", 5000, 80, ProtocolType.TCP, 2);

        loadBalancer.dispatch(p1);
        loadBalancer.dispatch(p2);

        // Wait for workers to process
        Thread.sleep(500);

        assertThat(loadBalancer.getDispatchedCount()).isEqualTo(2);

        // Find which worker got the packets
        WorkerService[] workers = loadBalancer.getWorkers();
        int targetIdx = FiveTupleHasher.computeWorkerIndex(p1, 2);
        assertThat(workers[targetIdx].getProcessedCount()).isEqualTo(2);
        assertThat(workers[targetIdx].getConnections()).hasSize(1);
    }

    @Test
    @DisplayName("Different connections can distribute across workers")
    void differentConnectionsDistribute() throws Exception {
        // Create packets with different five-tuples
        for (int i = 0; i < 20; i++) {
            ParsedPacket p = buildPacket("10.0." + (i % 10) + ".1", "10.0.0.2",
                    5000 + i, 80, ProtocolType.TCP, i);
            loadBalancer.dispatch(p);
        }

        Thread.sleep(500);

        assertThat(loadBalancer.getDispatchedCount()).isEqualTo(20);

        // Both workers should have received at least some packets
        WorkerService[] workers = loadBalancer.getWorkers();
        long totalProcessed = workers[0].getProcessedCount() + workers[1].getProcessedCount();
        assertThat(totalProcessed).isEqualTo(20);
    }

    @Test
    @DisplayName("Tracks connection state correctly per worker")
    void connectionTrackingPerWorker() throws Exception {
        String srcIp = "192.168.1.1";
        String destIp = "8.8.8.8";

        // Send 5 packets from same connection
        for (int i = 0; i < 5; i++) {
            loadBalancer.dispatch(buildPacket(srcIp, destIp, 12345, 443, ProtocolType.TCP, i));
        }

        Thread.sleep(500);

        int targetIdx = FiveTupleHasher.computeWorkerIndex(
                buildPacket(srcIp, destIp, 12345, 443, ProtocolType.TCP, 0), 2);
        WorkerService worker = loadBalancer.getWorkers()[targetIdx];

        assertThat(worker.getConnections()).hasSize(1);
        var conn = worker.getConnections().values().iterator().next();
        assertThat(conn.getPacketCount()).isEqualTo(5);
        assertThat(conn.getBytesTransferred()).isEqualTo(5 * 100L);
    }

    private ParsedPacket buildPacket(String srcIp, String destIp, int srcPort,
            int destPort, ProtocolType proto, int seq) {
        return ParsedPacket.builder()
                .srcIp(srcIp).destIp(destIp)
                .srcPort(srcPort).destPort(destPort)
                .protocol(proto)
                .packetSize(100).timestamp(Instant.now()).sequenceNumber(seq)
                .build();
    }
}
