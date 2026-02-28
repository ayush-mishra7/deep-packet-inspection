package com.ayush.dpi.loadbalancer;

import com.ayush.dpi.config.DpiProperties;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.rules.RuleRegistry;
import com.ayush.dpi.worker.WorkerService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Distributes parsed packets across worker threads using consistent
 * five-tuple hashing.
 */
@Slf4j
@Service
public class LoadBalancerService {

    private final DpiProperties properties;
    private final RuleRegistry ruleRegistry;
    private final com.ayush.dpi.stats.StatsService statsService;
    private WorkerService[] workers;
    private ExecutorService executorService;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicLong dispatchedCount = new AtomicLong(0);
    private final AtomicLong dropCount = new AtomicLong(0);

    public LoadBalancerService(DpiProperties properties, RuleRegistry ruleRegistry,
            com.ayush.dpi.stats.StatsService statsService) {
        this.properties = properties;
        this.ruleRegistry = ruleRegistry;
        this.statsService = statsService;
    }

    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("LoadBalancer already initialized, skipping");
            return;
        }

        int workerCount = properties.getWorker().getCount();
        int queueCapacity = properties.getWorker().getQueueCapacity();

        log.info("Initializing LoadBalancer: {} workers, queue capacity {}, {} active rules",
                workerCount, queueCapacity, ruleRegistry.size());

        workers = new WorkerService[workerCount];
        executorService = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("dpi-worker-" + t.getId());
            return t;
        });

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new WorkerService(i, queueCapacity, ruleRegistry, statsService);
            executorService.submit(workers[i]);
        }

        log.info("LoadBalancer initialized — {} workers running", workerCount);
    }

    public void dispatch(ParsedPacket packet) {
        if (!initialized.get()) {
            throw new IllegalStateException("LoadBalancer not initialized. Call init() first.");
        }

        int workerIndex = FiveTupleHasher.computeWorkerIndex(packet, workers.length);
        boolean accepted = workers[workerIndex].enqueue(packet);

        if (accepted) {
            dispatchedCount.incrementAndGet();
        } else {
            dropCount.incrementAndGet();
            log.warn("Worker-{} queue full, dropping packet #{}", workerIndex, packet.getSequenceNumber());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (!initialized.get())
            return;

        log.info("Shutting down LoadBalancer...");
        for (WorkerService worker : workers)
            worker.shutdown();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Workers did not terminate in 30s, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }

        log.info("════════════════════════════════════════");
        log.info("  LoadBalancer Shutdown Summary");
        log.info("  Dispatched : {}", dispatchedCount.get());
        log.info("  Dropped    : {}", dropCount.get());
        for (WorkerService worker : workers) {
            log.info("  Worker-{}   : {} packets, {} connections",
                    worker.getWorkerId(), worker.getProcessedCount(),
                    worker.getConnections().size());
        }
        log.info("════════════════════════════════════════");

        initialized.set(false);
    }

    public WorkerService[] getWorkers() {
        return workers;
    }

    public long getDispatchedCount() {
        return dispatchedCount.get();
    }

    public long getDropCount() {
        return dropCount.get();
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}
