package com.ayush.dpi.connection;

import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;

import java.util.Objects;

/**
 * A fast, allocation-free (once created) struct representing a five-tuple
 * connection key.
 * Used in high-performance {@link java.util.HashMap} lookups.
 * Prevents expensive string allocations on the hot path.
 */
public record ConnectionKey(
        String srcIp,
        String destIp,
        int srcPort,
        int destPort,
        ProtocolType protocol) {

    /**
     * Factory to build a ConnectionKey directly from a ParsedPacket.
     */
    public static ConnectionKey from(ParsedPacket packet) {
        return new ConnectionKey(
                packet.getSrcIp(),
                packet.getDestIp(),
                packet.getSrcPort(),
                packet.getDestPort(),
                packet.getProtocol());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConnectionKey that = (ConnectionKey) o;
        return srcPort == that.srcPort &&
                destPort == that.destPort &&
                protocol == that.protocol &&
                srcIp.equals(that.srcIp) &&
                destIp.equals(that.destIp);
    }

    @Override
    public int hashCode() {
        // Use Objects.hash for quick pre-computable hashing
        return Objects.hash(srcIp, destIp, srcPort, destPort, protocol);
    }
}
