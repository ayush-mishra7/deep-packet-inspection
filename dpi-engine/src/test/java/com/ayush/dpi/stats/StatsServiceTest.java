package com.ayush.dpi.stats;

import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class StatsServiceTest {

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Aggregates single local flush")
    void aggregatesSingleFlush() {
        WorkerLocalStats local = new WorkerLocalStats();
        local.record(pkt(ProtocolType.TCP, 100, "example.com"), Decision.ALLOW);
        local.record(pkt(ProtocolType.UDP, 50, "example.com"), Decision.BLOCK);

        statsService.recordMetrics(local);

        assertThat(statsService.getTotalPackets()).isEqualTo(2);
        assertThat(statsService.getTotalBytes()).isEqualTo(150);
        assertThat(statsService.getTcpPackets()).isEqualTo(1);
        assertThat(statsService.getUdpPackets()).isEqualTo(1);
        assertThat(statsService.getAllowedPackets()).isEqualTo(1);
        assertThat(statsService.getBlockedPackets()).isEqualTo(1);
        assertThat(statsService.getThrottledPackets()).isZero();

        assertThat(statsService.getDomainFrequency().get("example.com").sum()).isEqualTo(2);
    }

    @Test
    @DisplayName("Aggregates concurrently without race conditions")
    void aggregatesConcurrently() throws InterruptedException {
        int threads = 10;
        int recordsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                WorkerLocalStats local = new WorkerLocalStats();
                for (int j = 0; j < recordsPerThread; j++) {
                    local.record(pkt(ProtocolType.TCP, 10, "concurrent.com"), Decision.ALLOW);
                }
                statsService.recordMetrics(local);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(statsService.getTotalPackets()).isEqualTo(10000);
        assertThat(statsService.getTotalBytes()).isEqualTo(100000);
        assertThat(statsService.getTcpPackets()).isEqualTo(10000);
        assertThat(statsService.getAllowedPackets()).isEqualTo(10000);
        assertThat(statsService.getDomainFrequency().get("concurrent.com").sum()).isEqualTo(10000);
    }

    private ParsedPacket pkt(ProtocolType protocol, int size, String sni) {
        return ParsedPacket.builder()
                .protocol(protocol)
                .packetSize(size)
                .sni(sni)
                .build();
    }
}
