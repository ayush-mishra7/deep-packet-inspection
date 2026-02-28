package com.ayush.dpi.rules;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;

/**
 * A stateless, thread-safe rule that evaluates a packet in the context
 * of its connection and returns a traffic {@link Decision}.
 * <p>
 * This interface establishes the primary extension point for the DPI Engine.
 * External plugin modules can implement this structural interface to inject
 * custom threat-intelligence analysis, geo-blocking logic, or protocol
 * tracking.
 * </p>
 */
public interface Rule {

    /**
     * Evaluate this rule against the given packet and connection.
     *
     * @param packet     the current parsed packet
     * @param connection the connection this packet belongs to
     * @return the decision (ALLOW, BLOCK, or THROTTLE)
     */
    Decision evaluate(ParsedPacket packet, Connection connection);

    /**
     * @return unique name identifying this rule
     */
    String getName();

    /**
     * @return human-readable description of what this rule does
     */
    String getDescription();
}
