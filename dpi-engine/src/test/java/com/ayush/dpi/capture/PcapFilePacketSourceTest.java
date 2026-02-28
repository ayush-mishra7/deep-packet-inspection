package com.ayush.dpi.capture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link PcapFilePacketSource}.
 * <p>
 * Generates a minimal PCAP file and verifies packet reading. These tests
 * require npcap/libpcap native library to be installed. They will be
 * gracefully skipped if the native library is unavailable.
 * </p>
 */
class PcapFilePacketSourceTest {

    private static final int PACKET_COUNT = 5;
    private static boolean npcapAvailable;

    @TempDir
    Path tempDir;

    private File pcapFile;

    @BeforeAll
    static void checkNpcap() {
        try {
            Class.forName("org.pcap4j.core.NativeMappings");
            // Try to trigger native load
            org.pcap4j.core.Pcaps.findAllDevs();
            npcapAvailable = true;
        } catch (Throwable t) {
            npcapAvailable = false;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        pcapFile = tempDir.resolve("test.pcap").toFile();
        writeSyntheticPcap(pcapFile, PACKET_COUNT);
    }

    @Test
    @DisplayName("Reads all packets from a PCAP file and calls handler for each")
    void readsAllPackets() {
        assumeTrue(npcapAvailable, "Skipped: npcap/libpcap not installed");

        PcapFilePacketSource source = new PcapFilePacketSource(pcapFile.getAbsolutePath());
        List<RawPacket> received = new ArrayList<>();

        source.start(received::add);

        assertThat(received).hasSize(PACKET_COUNT);
        assertThat(source.getPacketCount()).isEqualTo(PACKET_COUNT);
        assertThat(source.isActive()).isFalse();
    }

    @Test
    @DisplayName("Each RawPacket has correct sequence number and non-empty data")
    void packetsHaveCorrectMetadata() {
        assumeTrue(npcapAvailable, "Skipped: npcap/libpcap not installed");

        PcapFilePacketSource source = new PcapFilePacketSource(pcapFile.getAbsolutePath());
        List<RawPacket> received = new ArrayList<>();

        source.start(received::add);

        for (int i = 0; i < received.size(); i++) {
            RawPacket pkt = received.get(i);
            assertThat(pkt.getSequenceNumber()).isEqualTo(i + 1);
            assertThat(pkt.getLength()).isGreaterThan(0);
            assertThat(pkt.getData()).isNotEmpty();
            assertThat(pkt.getTimestamp()).isNotNull();
        }
    }

    @Test
    @DisplayName("Throws exception for non-existent file")
    void throwsForMissingFile() {
        PcapFilePacketSource source = new PcapFilePacketSource("/nonexistent/file.pcap");

        assertThatThrownBy(() -> source.start(pkt -> {
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("Resource cleanup after processing")
    void resourceCleanup() {
        assumeTrue(npcapAvailable, "Skipped: npcap/libpcap not installed");

        PcapFilePacketSource source = new PcapFilePacketSource(pcapFile.getAbsolutePath());
        source.start(pkt -> {
        });

        assertThat(source.isActive()).isFalse();
        // Calling stop again should be safe (idempotent)
        source.stop();
        assertThat(source.isActive()).isFalse();
    }

    // =========================================================================
    // Helper — writes a minimal valid PCAP file with synthetic Ethernet frames
    // =========================================================================

    private void writeSyntheticPcap(File file, int numPackets) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Global PCAP header (24 bytes)
            ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(0xA1B2C3D4); // magic number
            header.putShort((short) 2); // version major
            header.putShort((short) 4); // version minor
            header.putInt(0); // timezone offset
            header.putInt(0); // timestamp accuracy
            header.putInt(65535); // snapshot length
            header.putInt(1); // link-layer type: Ethernet
            fos.write(header.array());

            for (int i = 0; i < numPackets; i++) {
                byte[] packetData = buildMinimalEthernetFrame(i);

                ByteBuffer pktHeader = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
                pktHeader.putInt(1700000000 + i);
                pktHeader.putInt(i * 1000);
                pktHeader.putInt(packetData.length);
                pktHeader.putInt(packetData.length);
                fos.write(pktHeader.array());
                fos.write(packetData);
            }
        }
    }

    private byte[] buildMinimalEthernetFrame(int index) {
        ByteBuffer frame = ByteBuffer.allocate(38);
        frame.put(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
        frame.put(new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 });
        frame.putShort((short) 0x0800);
        frame.put((byte) 0x45);
        frame.put((byte) 0x00);
        frame.putShort((short) 24);
        frame.putShort((short) index);
        frame.putShort((short) 0);
        frame.put((byte) 64);
        frame.put((byte) 6);
        frame.putShort((short) 0);
        frame.put(new byte[] { 10, 0, 0, 1 });
        frame.put(new byte[] { 10, 0, 0, 2 });
        frame.putInt(index);
        return frame.array();
    }
}
