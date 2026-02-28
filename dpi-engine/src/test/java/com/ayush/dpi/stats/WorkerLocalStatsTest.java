package com.ayush.dpi.stats;

import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerLocalStatsTest {

    private WorkerLocalStats stats;

    @BeforeEach
    void setUp() {
        stats = new WorkerLocalStats();
    }

    @Test
    @DisplayName("Records packets and maps decisions correctly")
    void recordsPacketsAndDecisions() {
        stats.record(pkt(ProtocolType.TCP, 100, "example.com"), Decision.ALLOW);
        stats.record(pkt(ProtocolType.TCP, 200, "example.com"), Decision.ALLOW);
        stats.record(pkt(ProtocolType.UDP, 50, null), Decision.BLOCK);
        stats.record(pkt(ProtocolType.OTHER, 20, "test.org"), Decision.THROTTLE);

        assertThat(stats.getPacketsProcessed()).isEqualTo(4);
        assertThat(stats.getBytesProcessed()).isEqualTo(370);
        assertThat(stats.getTcpPackets()).isEqualTo(2);
        assertThat(stats.getUdpPackets()).isEqualTo(1);

        assertThat(stats.getAllowedPackets()).isEqualTo(2);
        assertThat(stats.getBlockedPackets()).isEqualTo(1);
        assertThat(stats.getThrottledPackets()).isEqualTo(1);

        assertThat(stats.getDomainFrequency()).containsEntry("example.com", 2L);
        assertThat(stats.getDomainFrequency()).containsEntry("test.org", 1L);
    }

    @Test
    @DisplayName("Reset clears all fields")
    void resetClearsAll() {
        stats.record(pkt(ProtocolType.TCP, 100, "example.com"), Decision.ALLOW);
        stats.reset();

        assertThat(stats.getPacketsProcessed()).isZero();
        assertThat(stats.getBytesProcessed()).isZero();
        assertThat(stats.getTcpPackets()).isZero();
        assertThat(stats.getUdpPackets()).isZero();
        assertThat(stats.getAllowedPackets()).isZero();
        assertThat(stats.getBlockedPackets()).isZero();
        assertThat(stats.getThrottledPackets()).isZero();
        assertThat(stats.getDomainFrequency()).isEmpty();
    }

    private ParsedPacket pkt(ProtocolType protocol, int size, String sni) {
        return ParsedPacket.builder()
                .protocol(protocol)
                .packetSize(size)
                .sni(sni)
                .build();
    }
}
