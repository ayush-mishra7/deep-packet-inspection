package com.ayush.dpi.connection;

import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Represents a logical network connection tracked by the five-tuple.
 * <p>
 * Each worker thread maintains its own isolated set of connections,
 * so this class does not need synchronization.
 * </p>
 */
@Getter
@ToString
public class Connection {

    private final String fiveTupleKey;
    private final String srcIp;
    private final String destIp;
    private final int srcPort;
    private final int destPort;
    private final ProtocolType protocol;

    private long bytesTransferred;
    private long packetCount;
    private Instant firstSeen;
    private Instant lastSeen;
    private Decision lastDecision = Decision.ALLOW;

    public Connection(String fiveTupleKey, ParsedPacket initialPacket) {
        this.fiveTupleKey = fiveTupleKey;
        this.srcIp = initialPacket.getSrcIp();
        this.destIp = initialPacket.getDestIp();
        this.srcPort = initialPacket.getSrcPort();
        this.destPort = initialPacket.getDestPort();
        this.protocol = initialPacket.getProtocol();
        this.bytesTransferred = initialPacket.getPacketSize();
        this.packetCount = 1;
        this.firstSeen = initialPacket.getTimestamp();
        this.lastSeen = initialPacket.getTimestamp();
    }

    /**
     * Update this connection's statistics with a new packet.
     *
     * @param packet the parsed packet belonging to this connection
     */
    public void updateWith(ParsedPacket packet) {
        this.bytesTransferred += packet.getPacketSize();
        this.packetCount++;
        if (packet.getTimestamp() != null) {
            this.lastSeen = packet.getTimestamp();
        }
    }

    /**
     * Set the last rule evaluation decision for this connection.
     *
     * @param decision the decision from rule evaluation
     */
    public void setLastDecision(Decision decision) {
        this.lastDecision = decision;
    }
}
