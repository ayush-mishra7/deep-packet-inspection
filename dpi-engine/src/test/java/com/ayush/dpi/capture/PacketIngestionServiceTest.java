package com.ayush.dpi.capture;

import com.ayush.dpi.config.DpiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PacketIngestionService} using a mock {@link PacketSource}.
 */
class PacketIngestionServiceTest {

    private DpiProperties properties;
    private PacketIngestionService service;

    @BeforeEach
    void setUp() {
        properties = new DpiProperties();
        properties.getCapture().setBatchLogInterval(3); // log every 3 packets for test
        service = new PacketIngestionService(properties);
    }

    @Test
    @DisplayName("Forwards all packets from source to downstream handler")
    void forwardsAllPackets() {
        int packetCount = 10;
        MockPacketSource source = new MockPacketSource(packetCount);
        List<RawPacket> received = new ArrayList<>();

        service.ingest(source, received::add);

        assertThat(received).hasSize(packetCount);
        assertThat(service.getTotalPacketsProcessed()).isEqualTo(packetCount);
    }

    @Test
    @DisplayName("Handles zero packets without errors")
    void handlesEmptySource() {
        MockPacketSource source = new MockPacketSource(0);
        List<RawPacket> received = new ArrayList<>();

        service.ingest(source, received::add);

        assertThat(received).isEmpty();
        assertThat(service.getTotalPacketsProcessed()).isZero();
    }

    @Test
    @DisplayName("Resets packet count between ingestion runs")
    void resetsCountBetweenRuns() {
        MockPacketSource source1 = new MockPacketSource(5);
        service.ingest(source1, pkt -> {
        });
        assertThat(service.getTotalPacketsProcessed()).isEqualTo(5);

        MockPacketSource source2 = new MockPacketSource(3);
        service.ingest(source2, pkt -> {
        });
        assertThat(service.getTotalPacketsProcessed()).isEqualTo(3);
    }

    // =========================================================================
    // Mock PacketSource that generates synthetic packets in-memory
    // =========================================================================

    private static class MockPacketSource implements PacketSource {

        private final int packetCount;
        private volatile boolean active = false;

        MockPacketSource(int packetCount) {
            this.packetCount = packetCount;
        }

        @Override
        public void start(PacketHandler handler) {
            active = true;
            AtomicLong seq = new AtomicLong(0);

            for (int i = 0; i < packetCount && active; i++) {
                RawPacket pkt = RawPacket.builder()
                        .data(new byte[] { 0x01, 0x02, 0x03 })
                        .length(3)
                        .timestamp(java.time.Instant.now())
                        .sequenceNumber(seq.incrementAndGet())
                        .build();
                handler.handle(pkt);
            }

            active = false;
        }

        @Override
        public void stop() {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
