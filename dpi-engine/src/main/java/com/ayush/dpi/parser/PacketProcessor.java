package com.ayush.dpi.parser;

/**
 * Functional interface for downstream consumers of parsed packets.
 * <p>
 * The load balancer and connection tracker in future phases will implement
 * this interface to receive structured packet data from the parser.
 * </p>
 */
@FunctionalInterface
public interface PacketProcessor {

    /**
     * Process a fully parsed packet.
     *
     * @param packet the parsed network packet with extracted metadata
     */
    void process(ParsedPacket packet);
}
