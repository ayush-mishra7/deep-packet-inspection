package com.ayush.dpi.rules;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IpBlockRuleTest {

    @Test
    @DisplayName("Blocks traffic from a blocked source IP")
    void blocksSrcIp() {
        IpBlockRule rule = new IpBlockRule("test", Set.of("10.0.0.1"));
        ParsedPacket pkt = pkt("10.0.0.1", "8.8.8.8");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    @Test
    @DisplayName("Blocks traffic to a blocked destination IP")
    void blocksDestIp() {
        IpBlockRule rule = new IpBlockRule("test", Set.of("8.8.8.8"));
        ParsedPacket pkt = pkt("10.0.0.1", "8.8.8.8");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    @Test
    @DisplayName("Allows non-blocked IPs")
    void allowsNonBlockedIp() {
        IpBlockRule rule = new IpBlockRule("test", Set.of("192.168.1.1"));
        ParsedPacket pkt = pkt("10.0.0.1", "8.8.8.8");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.ALLOW);
    }

    private ParsedPacket pkt(String src, String dest) {
        return ParsedPacket.builder().srcIp(src).destIp(dest)
                .srcPort(1234).destPort(80).protocol(ProtocolType.TCP)
                .packetSize(100).timestamp(Instant.now()).sequenceNumber(1).build();
    }
}
