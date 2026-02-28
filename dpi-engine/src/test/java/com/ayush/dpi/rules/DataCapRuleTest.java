package com.ayush.dpi.rules;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DataCapRuleTest {

    @Test
    @DisplayName("Allows traffic under threshold")
    void allowsUnderThreshold() {
        DataCapRule rule = new DataCapRule("test", 1000);
        ParsedPacket pkt = pkt(100);
        Connection conn = new Connection(com.ayush.dpi.connection.ConnectionKey.from(pkt), pkt); // 100 bytes

        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.ALLOW);
    }

    @Test
    @DisplayName("Throttles traffic over threshold")
    void throttlesOverThreshold() {
        DataCapRule rule = new DataCapRule("test", 1000);
        ParsedPacket pkt = pkt(100);
        Connection conn = new Connection(com.ayush.dpi.connection.ConnectionKey.from(pkt), pkt);

        // Simulate growth to 1100 bytes
        for (int i = 0; i < 10; i++)
            conn.updateWith(pkt);

        assertThat(conn.getBytesTransferred()).isEqualTo(1100);
        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.THROTTLE);
    }

    @Test
    @DisplayName("Blocks traffic over 2x threshold")
    void blocksOver2xThreshold() {
        DataCapRule rule = new DataCapRule("test", 1000);
        ParsedPacket pkt = pkt(100);
        Connection conn = new Connection(com.ayush.dpi.connection.ConnectionKey.from(pkt), pkt);

        // Simulate growth to 2100 bytes
        for (int i = 0; i < 20; i++)
            conn.updateWith(pkt);

        assertThat(conn.getBytesTransferred()).isEqualTo(2100);
        assertThat(rule.evaluate(pkt, conn)).isEqualTo(Decision.BLOCK);
    }

    private ParsedPacket pkt(int size) {
        return ParsedPacket.builder().srcIp("10.0.0.1").destIp("8.8.8.8")
                .srcPort(1234).destPort(80).protocol(ProtocolType.TCP)
                .packetSize(size).timestamp(Instant.now()).sequenceNumber(1).build();
    }
}
