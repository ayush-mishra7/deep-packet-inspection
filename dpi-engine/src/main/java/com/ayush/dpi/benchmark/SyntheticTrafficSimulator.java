package com.ayush.dpi.benchmark;

import com.ayush.dpi.loadbalancer.LoadBalancerService;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates synthetic high-throughput traffic to stress test the DPI engine.
 * Useful for profiling CPU bottlenecks and measuring maximum packets per second
 * (PPS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticTrafficSimulator {

    private final LoadBalancerService loadBalancer;

    // A pool of typical destination IPs to simulate common web targets
    private static final String[] TARGET_IPS = {
            "8.8.8.8", "1.1.1.1", "142.250.190.46", "34.117.59.81", "104.244.42.1"
    };

    // A pool of typical SNIs for domain matching
    private static final String[] SNIS = {
            "google.com", "api.github.com", "cdn.discordapp.com", "evil.com", null
    };

    /**
     * Run a synthetic load test.
     *
     * @param totalPackets Total number of packets to inject
     * @param batchSize    Number of packets to generate before a tiny yield
     * @throws InterruptedException if thread is interrupted
     */
    public void runBenchmark(int totalPackets, int batchSize) throws InterruptedException {
        log.info("Starting Benchmark: injecting {} packets...", totalPackets);

        long startTimeNs = System.nanoTime();

        for (int i = 0; i < totalPackets; i++) {
            ParsedPacket packet = generateRandomPacket(i);
            loadBalancer.dispatch(packet);

            // Yield occasionally to let worker threads pull from the queues
            if (i > 0 && i % batchSize == 0) {
                Thread.yield();
            }
        }

        long durationNs = System.nanoTime() - startTimeNs;
        double durationSec = durationNs / 1_000_000_000.0;
        double pps = totalPackets / durationSec;

        log.info("Benchmark finished injection!");
        log.info("Time taken: {} seconds", String.format("%.3f", durationSec));
        log.info("Injection Rate: {} packets/sec", String.format("%,.0f", pps));
    }

    private ParsedPacket generateRandomPacket(int sequence) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // Generate a random /16 source IP space to simulate 65k distinct clients
        String srcIp = "10.0." + rnd.nextInt(256) + "." + rnd.nextInt(256);
        String destIp = TARGET_IPS[rnd.nextInt(TARGET_IPS.length)];

        int srcPort = 1024 + rnd.nextInt(60000);
        int destPort = rnd.nextBoolean() ? 443 : 80;

        ProtocolType protocol = rnd.nextInt(10) < 8 ? ProtocolType.TCP : ProtocolType.UDP;
        int packetSize = 64 + rnd.nextInt(1400); // 64 to ~1464 bytes

        String sni = null;
        if (destPort == 443 && protocol == ProtocolType.TCP && rnd.nextInt(10) < 3) {
            // 30% chance for an HTTPS packet to carry SNI
            sni = SNIS[rnd.nextInt(SNIS.length)];
        }

        return ParsedPacket.builder()
                .srcIp(srcIp)
                .destIp(destIp)
                .srcPort(srcPort)
                .destPort(destPort)
                .protocol(protocol)
                .packetSize(packetSize)
                .sequenceNumber(sequence)
                .timestamp(Instant.now())
                .sni(sni)
                .build();
    }
}
