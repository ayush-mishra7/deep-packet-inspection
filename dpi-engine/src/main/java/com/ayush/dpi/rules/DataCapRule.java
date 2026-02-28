package com.ayush.dpi.rules;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import lombok.Getter;

/**
 * Throttles or blocks connections that exceed a configurable data transfer
 * threshold.
 * <p>
 * Returns THROTTLE when bytes exceed the threshold. If the connection has
 * already
 * been throttled and continues to grow beyond 2x the threshold, returns BLOCK.
 * </p>
 */
@Getter
public class DataCapRule implements Rule {

    private final String name;
    private final long thresholdBytes;

    public DataCapRule(String name, long thresholdBytes) {
        this.name = name;
        this.thresholdBytes = thresholdBytes;
    }

    @Override
    public Decision evaluate(ParsedPacket packet, Connection connection) {
        long transferred = connection.getBytesTransferred();

        if (transferred > thresholdBytes * 2) {
            return Decision.BLOCK;
        }
        if (transferred > thresholdBytes) {
            return Decision.THROTTLE;
        }
        return Decision.ALLOW;
    }

    @Override
    public String getDescription() {
        return "Throttles at " + thresholdBytes + "B, blocks at " + (thresholdBytes * 2) + "B";
    }
}
