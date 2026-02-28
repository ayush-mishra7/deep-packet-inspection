package com.ayush.dpi.capture;

/**
 * Abstraction over a source of network packets.
 * <p>
 * Implementations may read from PCAP files, live network interfaces, or any
 * other packet source. Each implementation streams packets to the provided
 * {@link PacketHandler} without loading the entire dataset into memory.
 * </p>
 */
public interface PacketSource {

    /**
     * Begin reading packets and forward each one to the given handler.
     * <p>
     * For file-based sources, this method blocks until all packets are processed.
     * For live-capture sources, this method blocks until {@link #stop()} is called.
     * </p>
     *
     * @param handler callback invoked for each captured packet
     */
    void start(PacketHandler handler);

    /**
     * Signal the source to stop reading packets and release resources.
     */
    void stop();

    /**
     * @return {@code true} if the source is currently reading packets
     */
    boolean isActive();
}
