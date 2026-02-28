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

class DomainBlockRuleTest {

    @Test
    @DisplayName("Blocks exact domain match via SNI")
    void blocksExactDomain() {
        DomainBlockRule rule = new DomainBlockRule("test", Set.of("evil.com"));
        ParsedPacket pkt = pktWithSni("evil.com");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    @Test
    @DisplayName("Blocks wildcard domain match")
    void blocksWildcard() {
        DomainBlockRule rule = new DomainBlockRule("test", Set.of("*.example.com"));
        ParsedPacket pkt = pktWithSni("sub.example.com");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    @Test
    @DisplayName("Wildcard matches root domain too")
    void wildcardMatchesRoot() {
        DomainBlockRule rule = new DomainBlockRule("test", Set.of("*.example.com"));
        ParsedPacket pkt = pktWithSni("example.com");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    @Test
    @DisplayName("Allows non-matching domains")
    void allowsNonMatching() {
        DomainBlockRule rule = new DomainBlockRule("test", Set.of("evil.com"));
        ParsedPacket pkt = pktWithSni("good.com");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.ALLOW);
    }

    @Test
    @DisplayName("Allows packets without SNI")
    void allowsNoSni() {
        DomainBlockRule rule = new DomainBlockRule("test", Set.of("evil.com"));
        ParsedPacket pkt = pktWithSni(null);
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.ALLOW);
    }

    @Test
    @DisplayName("Case-insensitive matching")
    void caseInsensitive() {
        DomainBlockRule rule = new DomainBlockRule("test", Set.of("Evil.COM"));
        ParsedPacket pkt = pktWithSni("evil.com");
        Connection conn = new Connection("key", pkt);

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    private ParsedPacket pktWithSni(String sni) {
        return ParsedPacket.builder().srcIp("10.0.0.1").destIp("8.8.8.8")
                .srcPort(1234).destPort(443).protocol(ProtocolType.TCP)
                .packetSize(100).timestamp(Instant.now()).sequenceNumber(1)
                .sni(sni).build();
    }
}
