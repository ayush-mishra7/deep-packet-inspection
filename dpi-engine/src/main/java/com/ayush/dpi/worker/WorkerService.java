package com.ayush.dpi.worker;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.loadbalancer.FiveTupleHasher;
import com.ayush.dpi.parser.ParsedPacket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A single worker thread that processes packets from its own blocking queue.
 * <p>
 * Each worker maintains an isolated {@link HashMap} of connections keyed
 * by five-tuple. No shared mutable state exists between workers, so no
 * synchronization is required on the connection map.
 * </p>
 */
@Slf4j
public class WorkerService implements Runnable {

    @Getter
    private final int workerId;
    private final LinkedBlockingQueue<ParsedPacket> queue;
    private final Map<String, Connection> connections = new HashMap<>();
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Sentinel packet used to signal shutdown.
     */
    private static final ParsedPacket POISON_PILL = ParsedPacket.builder()
            .srcIp("0.0.0.0")
            .destIp("0.0.0.0")
            .srcPort(0)
            .destPort(0)
            .protocol(com.ayush.dpi.parser.ProtocolType.OTHER)
            .packetSize(0)
            .sequenceNumber(-1)
            .build();

    public WorkerService(int workerId, int queueCapacity) {
        this.workerId = workerId;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
    }

    /**
     * Enqueue a packet for this worker to process.
     *
     * @param packet the parsed packet
     * @return true if enqueued successfully, false if queue is full
     */
    public boolean enqueue(ParsedPacket packet) {
        return queue.offer(packet);
    }

    /**
     * Signal this worker to stop processing after draining its queue.
     */
    public void shutdown() {
        running.set(false);
        queue.offer(POISON_PILL);
    }

    @Override
    public void run() {
        running.set(true);
        log.info("Worker-{} started (queue capacity: {})", workerId, queue.remainingCapacity() + queue.size());

        while (running.get() || !queue.isEmpty()) {
            try {
                ParsedPacket packet = queue.take();

                // Check for poison pill
                if (packet.getSequenceNumber() == -1) {
                    log.debug("Worker-{} received shutdown signal", workerId);
                    break;
                }

                processPacket(packet);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Worker-{} interrupted", workerId);
                break;
            }
        }

        log.info("Worker-{} stopped — processed {} packets, {} connections tracked",
                workerId, processedCount.get(), connections.size());
    }

    private void processPacket(ParsedPacket packet) {
        String key = FiveTupleHasher.computeKey(packet);
        long count = processedCount.incrementAndGet();

        Connection conn = connections.get(key);
        if (conn == null) {
            conn = new Connection(key, packet);
            connections.put(key, conn);
            log.debug("Worker-{} | NEW connection: {} (packet #{})", workerId, key, count);
        } else {
            conn.updateWith(packet);
            log.debug("Worker-{} | UPDATED connection: {} | pkts={} bytes={} (packet #{})",
                    workerId, key, conn.getPacketCount(), conn.getBytesTransferred(), count);
        }
    }

    /**
     * @return total packets processed by this worker
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    /**
     * @return unmodifiable view of this worker's connection table
     */
    public Map<String, Connection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    /**
     * @return current queue depth
     */
    public int getQueueDepth() {
        return queue.size();
    }

    /**
     * @return the poison pill sentinel for testing
     */
    static ParsedPacket poisonPill() {
        return POISON_PILL;
    }
}
