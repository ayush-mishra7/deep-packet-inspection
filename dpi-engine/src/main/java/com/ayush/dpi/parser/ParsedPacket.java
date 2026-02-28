package com.ayush.dpi.parser;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Structured domain object representing a fully parsed network packet.
 * <p>
 * Contains extracted metadata from Ethernet, IP, and transport-layer headers,
 * plus optional TLS SNI when a ClientHello is detected.
 * </p>
 */
@Getter
@Builder
@ToString
public class ParsedPacket {

    /** Source IP address (IPv4 or IPv6 string representation). */
    private final String srcIp;

    /** Destination IP address (IPv4 or IPv6 string representation). */
    private final String destIp;

    /** Source port (0 for non-TCP/UDP protocols). */
    private final int srcPort;

    /** Destination port (0 for non-TCP/UDP protocols). */
    private final int destPort;

    /** Identified transport/network protocol. */
    private final ProtocolType protocol;

    /** Total packet size in bytes (as captured). */
    private final int packetSize;

    /** Packet capture timestamp. */
    private final Instant timestamp;

    /** TLS Server Name Indication — {@code null} if not a TLS ClientHello. */
    private final String sni;

    /** Sequence number inherited from the raw packet during ingestion. */
    private final long sequenceNumber;
}
