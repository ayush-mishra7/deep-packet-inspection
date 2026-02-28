package com.ayush.dpi.capture;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Immutable domain object representing a raw network packet before parsing.
 * <p>
 * Contains the raw byte payload, capture timestamp, packet length,
 * and a sequence number assigned during ingestion.
 * </p>
 */
@Getter
@Builder
@ToString(exclude = "data")
public class RawPacket {

    /** Raw packet bytes as captured from the source. */
    private final byte[] data;

    /** Total length of the packet in bytes. */
    private final int length;

    /** Timestamp when the packet was captured. */
    private final Instant timestamp;

    /** Monotonically increasing sequence number assigned during ingestion. */
    private final long sequenceNumber;
}
