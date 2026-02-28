package com.ayush.dpi.loadbalancer;

import com.ayush.dpi.parser.ParsedPacket;

/**
 * Computes a deterministic five-tuple key and worker index for a parsed packet.
 * <p>
 * The five-tuple consists of: srcIp, destIp, srcPort, destPort, protocol.
 * Using consistent hashing ensures all packets belonging to the same logical
 * connection are always routed to the same worker thread.
 * </p>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 */
public final class FiveTupleHasher {

    private FiveTupleHasher() {
        // Utility class
    }

    /**
     * Extract a deterministic key object from the five-tuple.
     *
     * @param packet the parsed packet
     * @return a ConnectionKey object
     */
    public static com.ayush.dpi.connection.ConnectionKey computeKey(ParsedPacket packet) {
        return com.ayush.dpi.connection.ConnectionKey.from(packet);
    }

    /**
     * Compute the target worker index for a packet using stable hash modulo.
     *
     * @param packet      the parsed packet
     * @param workerCount total number of workers
     * @return the worker index [0, workerCount)
     */
    public static int computeWorkerIndex(ParsedPacket packet, int workerCount) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }
        com.ayush.dpi.connection.ConnectionKey key = computeKey(packet);
        // Use Math.abs with a bitwise AND to avoid Integer.MIN_VALUE edge case
        int hash = key.hashCode() & 0x7FFFFFFF;
        return hash % workerCount;
    }
}
