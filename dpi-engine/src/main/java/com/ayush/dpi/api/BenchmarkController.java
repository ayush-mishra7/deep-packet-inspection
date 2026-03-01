package com.ayush.dpi.api;

import com.ayush.dpi.benchmark.SyntheticTrafficSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REST endpoint to trigger synthetic traffic injection for demo/screenshot purposes.
 * Works in server mode — injects packets through the pipeline without shutting down.
 */
@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final SyntheticTrafficSimulator simulator;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * POST /api/benchmark/run?packets=500000&batchSize=1000
     * Triggers a synthetic traffic burst. Returns immediately; injection runs async.
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runBenchmark(
            @RequestParam(defaultValue = "500000") int packets,
            @RequestParam(defaultValue = "1000") int batchSize) {

        if (running.get()) {
            return ResponseEntity.ok(Map.of(
                    "status", "already_running",
                    "message", "A benchmark injection is already in progress"
            ));
        }

        running.set(true);
        new Thread(() -> {
            try {
                simulator.runBenchmark(packets, batchSize);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Benchmark interrupted");
            } finally {
                running.set(false);
            }
        }, "benchmark-injector").start();

        return ResponseEntity.accepted().body(Map.of(
                "status", "started",
                "packets", packets,
                "batchSize", batchSize,
                "message", "Traffic injection started. Watch the dashboard at http://localhost:5173"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("running", running.get()));
    }
}
