package com.ayush.dpi.loadbalancer;

import com.ayush.dpi.config.DpiProperties;
import com.ayush.dpi.parser.ParsedPacket;
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
 * <p>
 * All packets belonging to the same logical connection (same five-tuple)
 * are guaranteed to be routed to the same worker, eliminating the need
 * for cross-thread synchronization on connection state.
 * </p>
 */
@Slf4j
@Service
public class LoadBalancerService {

    private final DpiProperties properties;
    private WorkerService[] workers;
    private ExecutorService executorService;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicLong dispatchedCount = new AtomicLong(0);
    private final AtomicLong dropCount = new AtomicLong(0);

    public LoadBalancerService(DpiProperties properties) {
        this.properties = properties;
    }

    /**
     * Initialize workers and start the thread pool.
     * Safe to call multiple times — only first call takes effect.
     */
    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("LoadBalancer already initialized, skipping");
            return;
        }

        int workerCount = properties.getWorker().getCount();
        int queueCapacity = properties.getWorker().getQueueCapacity();

        log.info("Initializing LoadBalancer: {} workers, queue capacity {}", workerCount, queueCapacity);

        workers = new WorkerService[workerCount];
        executorService = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("dpi-worker-" + t.getId());
            return t;
        });

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new WorkerService(i, queueCapacity);
            executorService.submit(workers[i]);
        }

        log.info("LoadBalancer initialized — {} workers running", workerCount);
    }

    /**
     * Dispatch a parsed packet to the appropriate worker based on five-tuple hash.
     *
     * @param packet the parsed packet to route
     */
    public void dispatch(ParsedPacket packet) {
        if (!initialized.get()) {
            throw new IllegalStateException("LoadBalancer not initialized. Call init() first.");
        }

        int workerIndex = FiveTupleHasher.computeWorkerIndex(packet, workers.length);
        boolean accepted = workers[workerIndex].enqueue(packet);

        if (accepted) {
            dispatchedCount.incrementAndGet();
            log.debug("Dispatched packet #{} to Worker-{}", packet.getSequenceNumber(), workerIndex);
        } else {
            dropCount.incrementAndGet();
            log.warn("Worker-{} queue full, dropping packet #{}", workerIndex, packet.getSequenceNumber());
        }
    }

    /**
     * Gracefully shut down all workers and the thread pool.
     * Sends poison pills to each worker, then waits for termination.
     */
    @PreDestroy
    public void shutdown() {
        if (!initialized.get()) {
            return;
        }

        log.info("Shutting down LoadBalancer...");

        // Signal all workers to stop
        for (WorkerService worker : workers) {
            worker.shutdown();
        }

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

    /**
     * @return the array of workers (for stats/testing access)
     */
    public WorkerService[] getWorkers() {
        return workers;
    }

    /**
     * @return total packets dispatched
     */
    public long getDispatchedCount() {
        return dispatchedCount.get();
    }

    /**
     * @return total packets dropped due to full queues
     */
    public long getDropCount() {
        return dropCount.get();
    }

    /**
     * @return whether the load balancer has been initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }
}
