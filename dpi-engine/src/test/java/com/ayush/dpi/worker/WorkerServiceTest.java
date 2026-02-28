package com.ayush.dpi.worker;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WorkerService}.
 */
class WorkerServiceTest {

    @Test
    @DisplayName("Processes enqueued packets and tracks connections")
    void processesPacketsAndTracksConnections() throws Exception {
        WorkerService worker = new WorkerService(0, 100, new com.ayush.dpi.rules.RuleRegistry());
        Thread thread = new Thread(worker);
        thread.start();

        // Enqueue 3 packets from same connection
        for (int i = 0; i < 3; i++) {
            worker.enqueue(buildPacket("10.0.0.1", "10.0.0.2", 5000, 80, ProtocolType.TCP, i));
        }

        // Enqueue 2 packets from a different connection
        for (int i = 0; i < 2; i++) {
            worker.enqueue(buildPacket("10.0.0.3", "10.0.0.4", 6000, 443, ProtocolType.TCP, 10 + i));
        }

        Thread.sleep(300);
        worker.shutdown();
        thread.join(2000);

        assertThat(worker.getProcessedCount()).isEqualTo(5);
        Map<String, Connection> connections = worker.getConnections();
        assertThat(connections).hasSize(2);

        // Verify first connection
        Connection conn1 = connections.values().stream()
                .filter(c -> c.getSrcIp().equals("10.0.0.1"))
                .findFirst().orElseThrow();
        assertThat(conn1.getPacketCount()).isEqualTo(3);
        assertThat(conn1.getBytesTransferred()).isEqualTo(300L);

        // Verify second connection
        Connection conn2 = connections.values().stream()
                .filter(c -> c.getSrcIp().equals("10.0.0.3"))
                .findFirst().orElseThrow();
        assertThat(conn2.getPacketCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Shuts down cleanly with empty queue")
    void shutsDownCleanlyWhenEmpty() throws Exception {
        WorkerService worker = new WorkerService(1, 100, new com.ayush.dpi.rules.RuleRegistry());
        Thread thread = new Thread(worker);
        thread.start();

        Thread.sleep(100);
        worker.shutdown();
        thread.join(2000);

        assertThat(worker.getProcessedCount()).isZero();
        assertThat(worker.getConnections()).isEmpty();
    }

    @Test
    @DisplayName("Returns false when queue is full")
    void returnsFalseWhenQueueFull() {
        WorkerService worker = new WorkerService(2, 2, new com.ayush.dpi.rules.RuleRegistry());

        boolean first = worker.enqueue(buildPacket("1.1.1.1", "2.2.2.2", 1, 2, ProtocolType.UDP, 1));
        boolean second = worker.enqueue(buildPacket("1.1.1.1", "2.2.2.2", 1, 2, ProtocolType.UDP, 2));
        boolean third = worker.enqueue(buildPacket("1.1.1.1", "2.2.2.2", 1, 2, ProtocolType.UDP, 3));

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(third).isFalse(); // queue capacity = 2
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
