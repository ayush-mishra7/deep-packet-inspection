package com.ayush.dpi.api;

import com.ayush.dpi.config.DpiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Health check controller exposing system liveness information.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DpiProperties dpiProperties;

    /**
     * Returns structured JSON with current engine status, timestamp, and version.
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        log.debug("Health check requested");

        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .timestamp(Instant.now())
                .version(dpiProperties.getVersion())
                .build();

        return ResponseEntity.ok(response);
    }
}
