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
 * A highly concurrent, isolated worker thread instance responsible for
 * processing parsed packets.
 * <p>
 * Each worker maintains a localized cache of connection states to ensure
 * completely isolated
 * memory access paths, avoiding cross-thread locks during hot packet
 * processing. It periodically
 * flushes its local statistics to the global
 * {@link com.ayush.dpi.stats.StatsService},
 * and evaluates rules from the shared {@link RuleRegistry}.
 * </p>
 */
@Slf4j
public class WorkerService implements Runnable {

    @Getter
    private final int workerId;
    private final LinkedBlockingQueue<ParsedPacket> queue;
    private final Map<com.ayush.dpi.connection.ConnectionKey, Connection> connections = new HashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final RuleRegistry ruleRegistry;
    private final com.ayush.dpi.stats.StatsService statsService;
    private final com.ayush.dpi.persistence.AuditEventPublisher auditEventPublisher;
    private final com.ayush.dpi.stats.WorkerLocalStats localStats = new com.ayush.dpi.stats.WorkerLocalStats();

    // Flush to global StatsService every N packets to minimize contention
    private static final int STATS_FLUSH_INTERVAL = 1000;

    private static final ParsedPacket POISON_PILL = ParsedPacket.builder()
            .srcIp("0.0.0.0").destIp("0.0.0.0")
            .srcPort(0).destPort(0)
            .protocol(com.ayush.dpi.parser.ProtocolType.OTHER)
            .packetSize(0).sequenceNumber(-1)
            .build();

    // Need to keep a running total because localStats resets on flush
    private final AtomicLong totalProcessedCount = new AtomicLong(0);

    public WorkerService(int workerId, int queueCapacity, RuleRegistry ruleRegistry,
            com.ayush.dpi.stats.StatsService statsService,
            com.ayush.dpi.persistence.AuditEventPublisher auditEventPublisher) {
        this.workerId = workerId;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.ruleRegistry = ruleRegistry;
        this.statsService = statsService;
        this.auditEventPublisher = auditEventPublisher;
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
                    log.trace("Worker-{} received shutdown signal", workerId);
                    break;
                }

                try {
                    processPacket(packet);
                } catch (Exception e) {
                    log.error("Worker-{} | Error processing packet #{}: {}", workerId, packet.getSequenceNumber(),
                            e.getMessage(), e);
                    localStats.recordError();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Worker-{} interrupted", workerId);
                break;
            }
        }

        // Final flush on shutdown
        flushStats();

        log.info("Worker-{} stopped — processed {} packets, {} connections tracked",
                workerId, localStats.getPacketsProcessed(), connections.size());
    }

    private void processPacket(ParsedPacket packet) {
        com.ayush.dpi.connection.ConnectionKey key = FiveTupleHasher.computeKey(packet);
        totalProcessedCount.incrementAndGet();

        // Update connection tracking
        Connection conn = connections.get(key);
        if (conn == null) {
            conn = new Connection(key, packet);
            connections.put(key, conn);
            log.trace("Worker-{} | NEW connection: {}", workerId, key);
        } else {
            conn.updateWith(packet);
        }

        // Evaluate rules
        Decision decision = evaluateRules(packet, conn);
        conn.setLastDecision(decision);

        // Record metrics locally
        localStats.record(packet, decision);

        // Periodic flush to avoid overwhelming central StatsService
        if (localStats.getPacketsProcessed() % STATS_FLUSH_INTERVAL == 0) {
            flushStats();
        }

        // Decrease hotpath logging verbosity to debug to improve performance
        if (decision != Decision.ALLOW) {
            log.trace("Worker-{} | {} | connection={} | pkts={} bytes={}",
                    workerId, decision, key, conn.getPacketCount(), conn.getBytesTransferred());
        }
    }

    private void flushStats() {
        if (localStats.getPacketsProcessed() > 0) {
            statsService.recordMetrics(localStats);
            localStats.reset();
        }
    }

    private Decision evaluateRules(ParsedPacket packet, Connection connection) {
        List<Rule> rules = ruleRegistry.getSnapshot();
        for (Rule rule : rules) {
            Decision decision = rule.evaluate(packet, connection);
            if (decision != Decision.ALLOW) {
                log.trace("Worker-{} | Rule [{}] triggered: {}", workerId, rule.getName(), decision);
                if (auditEventPublisher != null) {
                    auditEventPublisher.publishRuleMatch(packet, rule, decision);
                }
                return decision;
            }
        }
        return Decision.ALLOW;
    }

    public long getProcessedCount() {
        return totalProcessedCount.get();
    }

    public Map<com.ayush.dpi.connection.ConnectionKey, Connection> getConnections() {
        return Collections.unmodifiableMap(connections);
    }

    public int getQueueDepth() {
        return queue.size();
    }

    static ParsedPacket poisonPill() {
        return POISON_PILL;
    }
}
