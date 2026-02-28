package com.ayush.dpi.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Structured response returned by the health endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /** Current system status (UP / DEGRADED / DOWN). */
    private String status;

    /** ISO-8601 timestamp of the health check. */
    private Instant timestamp;

    /** Application version string. */
    private String version;
}
