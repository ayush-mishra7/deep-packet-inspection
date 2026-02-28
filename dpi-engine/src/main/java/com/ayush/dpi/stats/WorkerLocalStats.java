package com.ayush.dpi.stats;

import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight, thread-unsafe stats collector used locally by a single worker.
 * <p>
 * This prevents cross-thread lock contention by letting workers accrue
 * stats locally and periodically flush them to the central
 * {@link StatsService}.
 * </p>
 */
@Getter
public class WorkerLocalStats {

    private long packetsProcessed = 0;
    private long bytesProcessed = 0;
    private long tcpPackets = 0;
    private long udpPackets = 0;

    private long allowedPackets = 0;
    private long blockedPackets = 0;
    private long throttledPackets = 0;

    private final Map<String, Long> domainFrequency = new HashMap<>();

    /**
     * Increment local counters for a processed packet and its resulting decision.
     */
    public void record(ParsedPacket packet, Decision decision) {
        packetsProcessed++;
        bytesProcessed += packet.getPacketSize();

        if (packet.getProtocol() == ProtocolType.TCP)
            tcpPackets++;
        else if (packet.getProtocol() == ProtocolType.UDP)
            udpPackets++;

        switch (decision) {
            case ALLOW -> allowedPackets++;
            case BLOCK -> blockedPackets++;
            case THROTTLE -> throttledPackets++;
        }

        if (packet.getSni() != null && !packet.getSni().isBlank()) {
            domainFrequency.merge(packet.getSni(), 1L, Long::sum);
        }
    }

    /**
     * Resets all local counters back to zero after flushing.
     */
    public void reset() {
        packetsProcessed = 0;
        bytesProcessed = 0;
        tcpPackets = 0;
        udpPackets = 0;
        allowedPackets = 0;
        blockedPackets = 0;
        throttledPackets = 0;
        domainFrequency.clear();
    }
}
