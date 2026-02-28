package com.ayush.dpi.capture;

/**
 * Functional interface for handling raw packets received from a
 * {@link PacketSource}.
 * <p>
 * Implementations process individual packets — for example, forwarding them to
 * the
 * parser layer, updating statistics, or writing to a log.
 * </p>
 */
@FunctionalInterface
public interface PacketHandler {

    /**
     * Invoked for each raw packet captured from a packet source.
     *
     * @param packet the raw packet data
     */
    void handle(RawPacket packet);
}
