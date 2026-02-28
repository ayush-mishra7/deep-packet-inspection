package com.ayush.dpi.parser;

import com.ayush.dpi.capture.RawPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PacketParserService}.
 * <p>
 * Uses synthetically constructed Ethernet frames to test parsing logic
 * without requiring native pcap libraries.
 * </p>
 */
class PacketParserServiceTest {

    private PacketParserService parser;

    @BeforeEach
    void setUp() {
        parser = new PacketParserService();
    }

    @Test
    @DisplayName("Parses IPv4 TCP packet correctly")
    void parsesIpv4TcpPacket() {
        byte[] frame = buildEthernetIpv4TcpFrame(
                "192.168.1.10", "10.0.0.1", 54321, 80, new byte[] { 0x01, 0x02 });

        RawPacket raw = RawPacket.builder()
                .data(frame)
                .length(frame.length)
                .timestamp(Instant.now())
                .sequenceNumber(1)
                .build();

        Optional<ParsedPacket> result = parser.parse(raw);

        assertThat(result).isPresent();
        ParsedPacket pkt = result.get();
        assertThat(pkt.getSrcIp()).isEqualTo("192.168.1.10");
        assertThat(pkt.getDestIp()).isEqualTo("10.0.0.1");
        assertThat(pkt.getSrcPort()).isEqualTo(54321);
        assertThat(pkt.getDestPort()).isEqualTo(80);
        assertThat(pkt.getProtocol()).isEqualTo(ProtocolType.TCP);
        assertThat(pkt.getPacketSize()).isEqualTo(frame.length);
        assertThat(pkt.getSni()).isNull();
        assertThat(pkt.getSequenceNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Parses IPv4 UDP packet correctly")
    void parsesIpv4UdpPacket() {
        byte[] frame = buildEthernetIpv4UdpFrame(
                "172.16.0.5", "8.8.8.8", 12345, 53, new byte[] { 0x00 });

        RawPacket raw = RawPacket.builder()
                .data(frame)
                .length(frame.length)
                .timestamp(Instant.now())
                .sequenceNumber(2)
                .build();

        Optional<ParsedPacket> result = parser.parse(raw);

        assertThat(result).isPresent();
        ParsedPacket pkt = result.get();
        assertThat(pkt.getSrcIp()).isEqualTo("172.16.0.5");
        assertThat(pkt.getDestIp()).isEqualTo("8.8.8.8");
        assertThat(pkt.getSrcPort()).isEqualTo(12345);
        assertThat(pkt.getDestPort()).isEqualTo(53);
        assertThat(pkt.getProtocol()).isEqualTo(ProtocolType.UDP);
    }

    @Test
    @DisplayName("Returns empty for null raw packet")
    void returnsEmptyForNull() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for empty data")
    void returnsEmptyForEmptyData() {
        RawPacket raw = RawPacket.builder()
                .data(new byte[0])
                .length(0)
                .timestamp(Instant.now())
                .sequenceNumber(3)
                .build();

        assertThat(parser.parse(raw)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for malformed/garbage data without throwing")
    void handlesGarbageDataGracefully() {
        RawPacket raw = RawPacket.builder()
                .data(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04 })
                .length(5)
                .timestamp(Instant.now())
                .sequenceNumber(4)
                .build();

        assertThat(parser.parse(raw)).isEmpty();
    }

    // =========================================================================
    // Frame builders — construct valid Ethernet/IPv4/TCP|UDP frames
    // =========================================================================

    private byte[] buildEthernetIpv4TcpFrame(
            String srcIp, String destIp, int srcPort, int destPort, byte[] payload) {

        byte[] tcpHeader = buildTcpHeader(srcPort, destPort);
        byte[] tcpSegment = concat(tcpHeader, payload);
        byte[] ipPacket = buildIpv4Header(srcIp, destIp, 6, tcpSegment.length);
        byte[] ipFull = concat(ipPacket, tcpSegment);
        byte[] ethHeader = buildEthernetHeader((short) 0x0800);
        return concat(ethHeader, ipFull);
    }

    private byte[] buildEthernetIpv4UdpFrame(
            String srcIp, String destIp, int srcPort, int destPort, byte[] payload) {

        byte[] udpHeader = buildUdpHeader(srcPort, destPort, payload.length);
        byte[] udpDatagram = concat(udpHeader, payload);
        byte[] ipPacket = buildIpv4Header(srcIp, destIp, 17, udpDatagram.length);
        byte[] ipFull = concat(ipPacket, udpDatagram);
        byte[] ethHeader = buildEthernetHeader((short) 0x0800);
        return concat(ethHeader, ipFull);
    }

    private byte[] buildEthernetHeader(short etherType) {
        ByteBuffer buf = ByteBuffer.allocate(14);
        buf.put(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }); // dst
        buf.put(new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 }); // src
        buf.putShort(etherType);
        return buf.array();
    }

    private byte[] buildIpv4Header(String srcIp, String destIp, int protocol, int payloadLen) {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.put((byte) 0x45); // version 4, IHL 5
        buf.put((byte) 0x00); // DSCP/ECN
        buf.putShort((short) (20 + payloadLen)); // total length
        buf.putShort((short) 0); // identification
        buf.putShort((short) 0x4000); // flags: Don't Fragment
        buf.put((byte) 64); // TTL
        buf.put((byte) protocol); // protocol
        buf.putShort((short) 0); // checksum (zero for test)
        buf.put(ipToBytes(srcIp));
        buf.put(ipToBytes(destIp));
        return buf.array();
    }

    private byte[] buildTcpHeader(int srcPort, int destPort) {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.putShort((short) srcPort);
        buf.putShort((short) destPort);
        buf.putInt(0); // sequence number
        buf.putInt(0); // acknowledgement number
        buf.putShort((short) 0x5000); // data offset (5 words) + flags
        buf.putShort((short) 65535); // window size
        buf.putShort((short) 0); // checksum
        buf.putShort((short) 0); // urgent pointer
        return buf.array();
    }

    private byte[] buildUdpHeader(int srcPort, int destPort, int payloadLen) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putShort((short) srcPort);
        buf.putShort((short) destPort);
        buf.putShort((short) (8 + payloadLen)); // length
        buf.putShort((short) 0); // checksum
        return buf.array();
    }

    private byte[] ipToBytes(String ip) {
        String[] parts = ip.split("\\.");
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i]);
        }
        return bytes;
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
