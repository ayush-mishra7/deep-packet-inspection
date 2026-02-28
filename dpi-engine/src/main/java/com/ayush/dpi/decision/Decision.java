package com.ayush.dpi.decision;

/**
 * Represents the traffic enforcement decision for a packet/connection.
 */
public enum Decision {
    /** Allow the traffic to pass through. */
    ALLOW,
    /** Block the traffic entirely. */
    BLOCK,
    /** Throttle the traffic (rate-limit or mark for QoS). */
    THROTTLE
}
