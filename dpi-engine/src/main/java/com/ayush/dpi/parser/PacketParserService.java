package com.ayush.dpi.parser;

import com.ayush.dpi.capture.RawPacket;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.namednumber.EtherType;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Stateless, thread-safe service that parses raw packet bytes into
 * structured {@link ParsedPacket} objects.
 * <p>
 * Decodes Ethernet → IP (v4/v6) → TCP/UDP headers and extracts
 * TLS SNI when a ClientHello is detected on TCP port 443.
 * Malformed or unsupported packets are logged at DEBUG level and skipped.
 * </p>
 */
@Slf4j
@Service
public class PacketParserService {

    /**
     * Parse a raw packet into a structured {@link ParsedPacket}.
     *
     * @param raw the raw packet from the capture layer
     * @return parsed packet if successful, empty if unsupported or corrupted
     */
    public Optional<ParsedPacket> parse(RawPacket raw) {
        if (raw == null || raw.getData() == null || raw.getData().length == 0) {
            log.debug("Skipping null or empty raw packet");
            return Optional.empty();
        }

        try {
            EthernetPacket ethernet = EthernetPacket.newPacket(raw.getData(), 0, raw.getData().length);
            return parseEthernet(ethernet, raw);
        } catch (IllegalRawDataException e) {
            log.debug("Packet #{} — malformed Ethernet frame: {}", raw.getSequenceNumber(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Packet #{} — unexpected parse error: {}", raw.getSequenceNumber(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ParsedPacket> parseEthernet(EthernetPacket ethernet, RawPacket raw) {
        EtherType etherType = ethernet.getHeader().getType();

        if (EtherType.IPV4.equals(etherType)) {
            IpV4Packet ipv4 = ethernet.get(IpV4Packet.class);
            if (ipv4 == null) {
                log.debug("Packet #{} — EtherType IPv4 but no IPv4 payload", raw.getSequenceNumber());
                return Optional.empty();
            }
            return parseIpV4(ipv4, raw);
        }

        if (EtherType.IPV6.equals(etherType)) {
            IpV6Packet ipv6 = ethernet.get(IpV6Packet.class);
            if (ipv6 == null) {
                log.debug("Packet #{} — EtherType IPv6 but no IPv6 payload", raw.getSequenceNumber());
                return Optional.empty();
            }
            return parseIpV6(ipv6, raw);
        }

        log.debug("Packet #{} — unsupported EtherType: {}", raw.getSequenceNumber(), etherType);
        return Optional.empty();
    }

    private Optional<ParsedPacket> parseIpV4(IpV4Packet ipv4, RawPacket raw) {
        IpV4Packet.IpV4Header header = ipv4.getHeader();
        String srcIp = header.getSrcAddr().getHostAddress();
        String destIp = header.getDstAddr().getHostAddress();
        int protocolNumber = header.getProtocol().value() & 0xFF;
        ProtocolType protocol = ProtocolType.fromIpProtocolNumber(protocolNumber);

        return extractTransportAndBuild(ipv4, srcIp, destIp, protocol, raw);
    }

    private Optional<ParsedPacket> parseIpV6(IpV6Packet ipv6, RawPacket raw) {
        IpV6Packet.IpV6Header header = ipv6.getHeader();
        String srcIp = header.getSrcAddr().getHostAddress();
        String destIp = header.getDstAddr().getHostAddress();
        int protocolNumber = header.getNextHeader().value() & 0xFF;
        ProtocolType protocol = ProtocolType.fromIpProtocolNumber(protocolNumber);

        return extractTransportAndBuild(ipv6, srcIp, destIp, protocol, raw);
    }

    private Optional<ParsedPacket> extractTransportAndBuild(
            Packet ipPacket, String srcIp, String destIp,
            ProtocolType protocol, RawPacket raw) {

        int srcPort = 0;
        int destPort = 0;
        String sni = null;

        if (protocol == ProtocolType.TCP) {
            TcpPacket tcp = ipPacket.get(TcpPacket.class);
            if (tcp != null) {
                srcPort = tcp.getHeader().getSrcPort().valueAsInt();
                destPort = tcp.getHeader().getDstPort().valueAsInt();
                sni = extractSni(tcp, destPort);
            }
        } else if (protocol == ProtocolType.UDP) {
            UdpPacket udp = ipPacket.get(UdpPacket.class);
            if (udp != null) {
                srcPort = udp.getHeader().getSrcPort().valueAsInt();
                destPort = udp.getHeader().getDstPort().valueAsInt();
            }
        }

        ParsedPacket parsed = ParsedPacket.builder()
                .srcIp(srcIp)
                .destIp(destIp)
                .srcPort(srcPort)
                .destPort(destPort)
                .protocol(protocol)
                .packetSize(raw.getLength())
                .timestamp(raw.getTimestamp())
                .sni(sni)
                .sequenceNumber(raw.getSequenceNumber())
                .build();

        return Optional.of(parsed);
    }

    private String extractSni(TcpPacket tcp, int destPort) {
        // Only attempt SNI extraction on likely TLS ports
        if (destPort != 443 && destPort != 8443) {
            return null;
        }

        Packet payload = tcp.getPayload();
        if (payload == null) {
            return null;
        }

        byte[] payloadBytes = payload.getRawData();
        if (payloadBytes == null || payloadBytes.length == 0) {
            return null;
        }

        return SniExtractor.extract(payloadBytes).orElse(null);
    }
}
