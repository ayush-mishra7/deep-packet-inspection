package com.ayush.dpi.capture;

/**
 * Abstraction over a source mechanism providing inbound network packets.
 * <p>
 * Implementations may interpret raw PCAP files, tail active network interface
 * cards in real-time mode, or bridge from an external messaging topic. Each
 * implementation streams encapsulated payload instances to the provided
 * callback
 * without attempting to load the entirety of the packet context into heap
 * memory.
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
