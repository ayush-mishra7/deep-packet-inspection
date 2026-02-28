package com.ayush.dpi.worker;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.loadbalancer.FiveTupleHasher;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.rules.Rule;
import com.ayush.dpi.rules.RuleRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A single worker thread that processes packets from its own blocking queue.
 * <p>
 * Each worker maintains an isolated connection map and evaluates rules
 * from the shared {@link RuleRegistry} after updating connection stats.
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
    private final RuleRegistry ruleRegistry;

    private static final ParsedPacket POISON_PILL = ParsedPacket.builder()
            .srcIp("0.0.0.0").destIp("0.0.0.0")
            .srcPort(0).destPort(0)
            .protocol(com.ayush.dpi.parser.ProtocolType.OTHER)
            .packetSize(0).sequenceNumber(-1)
            .build();

    public WorkerService(int workerId, int queueCapacity, RuleRegistry ruleRegistry) {
        this.workerId = workerId;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.ruleRegistry = ruleRegistry;
    }

    public boolean enqueue(ParsedPacket packet) {
        return queue.offer(packet);
    }

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

        // Update connection tracking
        Connection conn = connections.get(key);
        if (conn == null) {
            conn = new Connection(key, packet);
            connections.put(key, conn);
            log.debug("Worker-{} | NEW connection: {} (packet #{})", workerId, key, count);
        } else {
            conn.updateWith(packet);
        }

        // Evaluate rules
        Decision decision = evaluateRules(packet, conn);
        conn.setLastDecision(decision);

        if (decision != Decision.ALLOW) {
            log.info("Worker-{} | {} | connection={} | pkts={} bytes={}",
                    workerId, decision, key, conn.getPacketCount(), conn.getBytesTransferred());
        } else {
            log.debug("Worker-{} | ALLOW | connection={} | pkts={} bytes={}",
                    workerId, key, conn.getPacketCount(), conn.getBytesTransferred());
        }
    }

    private Decision evaluateRules(ParsedPacket packet, Connection connection) {
        List<Rule> rules = ruleRegistry.getSnapshot();
        for (Rule rule : rules) {
            Decision decision = rule.evaluate(packet, connection);
            if (decision != Decision.ALLOW) {
                log.debug("Worker-{} | Rule [{}] triggered: {}", workerId, rule.getName(), decision);
                return decision;
            }
        }
        return Decision.ALLOW;
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public Map<String, Connection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    public int getQueueDepth() {
        return queue.size();
    }

    static ParsedPacket poisonPill() {
        return POISON_PILL;
    }
}
