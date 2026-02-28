package com.ayush.dpi.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Structured error response returned by the global exception handler.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** ISO-8601 timestamp when the error occurred. */
    private Instant timestamp;

    /** HTTP status code. */
    private int status;

    /** Human-readable error type. */
    private String error;

    /** Detailed error message. */
    private String message;

    /** Request path that triggered the error. */
    private String path;
}
