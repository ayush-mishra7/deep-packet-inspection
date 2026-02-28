package com.ayush.dpi.loadbalancer;

import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FiveTupleHasher}.
 */
class FiveTupleHasherTest {

    @Test
    @DisplayName("Same five-tuple always produces the same key")
    void sameKeyForSameFiveTuple() {
        ParsedPacket p1 = buildPacket("10.0.0.1", "10.0.0.2", 1234, 80, ProtocolType.TCP);
        ParsedPacket p2 = buildPacket("10.0.0.1", "10.0.0.2", 1234, 80, ProtocolType.TCP);

        assertThat(FiveTupleHasher.computeKey(p1)).isEqualTo(FiveTupleHasher.computeKey(p2));
    }

    @Test
    @DisplayName("Different five-tuples produce different keys")
    void differentKeyForDifferentFiveTuple() {
        ParsedPacket p1 = buildPacket("10.0.0.1", "10.0.0.2", 1234, 80, ProtocolType.TCP);
        ParsedPacket p2 = buildPacket("10.0.0.1", "10.0.0.3", 1234, 80, ProtocolType.TCP);

        assertThat(FiveTupleHasher.computeKey(p1)).isNotEqualTo(FiveTupleHasher.computeKey(p2));
    }

    @Test
    @DisplayName("Same five-tuple always maps to the same worker index")
    void deterministicWorkerIndex() {
        ParsedPacket packet = buildPacket("192.168.1.1", "8.8.8.8", 54321, 443, ProtocolType.TCP);

        int idx1 = FiveTupleHasher.computeWorkerIndex(packet, 4);
        int idx2 = FiveTupleHasher.computeWorkerIndex(packet, 4);
        int idx3 = FiveTupleHasher.computeWorkerIndex(packet, 4);

        assertThat(idx1).isEqualTo(idx2).isEqualTo(idx3);
    }

    @Test
    @DisplayName("Worker index is within valid range")
    void workerIndexInRange() {
        for (int i = 0; i < 100; i++) {
            ParsedPacket p = buildPacket("10.0.0." + (i % 255), "10.0.1." + (i % 255),
                    1000 + i, 80 + (i % 10), ProtocolType.TCP);
            int index = FiveTupleHasher.computeWorkerIndex(p, 8);
            assertThat(index).isBetween(0, 7);
        }
    }

    @Test
    @DisplayName("Different protocols produce different keys")
    void differentProtocolsDifferentKeys() {
        ParsedPacket tcp = buildPacket("10.0.0.1", "10.0.0.2", 1234, 80, ProtocolType.TCP);
        ParsedPacket udp = buildPacket("10.0.0.1", "10.0.0.2", 1234, 80, ProtocolType.UDP);

        assertThat(FiveTupleHasher.computeKey(tcp)).isNotEqualTo(FiveTupleHasher.computeKey(udp));
    }

    private ParsedPacket buildPacket(String srcIp, String destIp, int srcPort, int destPort, ProtocolType proto) {
        return ParsedPacket.builder()
                .srcIp(srcIp).destIp(destIp)
                .srcPort(srcPort).destPort(destPort)
                .protocol(proto)
                .packetSize(100).timestamp(Instant.now()).sequenceNumber(1)
                .build();
    }
}
