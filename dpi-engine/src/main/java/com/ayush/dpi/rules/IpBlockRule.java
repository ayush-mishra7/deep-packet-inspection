package com.ayush.dpi.rules;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Blocks traffic from or to any IP address in a configurable blocklist.
 * <p>
 * Stateless and thread-safe — the blocked IP set is immutable after
 * construction.
 * </p>
 */
@Getter
public class IpBlockRule implements Rule {

    private final String name;
    private final Set<String> blockedIps;

    public IpBlockRule(String name, Set<String> blockedIps) {
        this.name = name;
        this.blockedIps = Collections.unmodifiableSet(new HashSet<>(blockedIps));
    }

    @Override
    public Decision evaluate(ParsedPacket packet, Connection connection) {
        if (blockedIps.contains(packet.getSrcIp()) || blockedIps.contains(packet.getDestIp())) {
            return Decision.BLOCK;
        }
        return Decision.ALLOW;
    }

    @Override
    public String getDescription() {
        return "Blocks traffic from/to IPs: " + blockedIps;
    }
}
