package com.ayush.dpi.rules;

import com.ayush.dpi.connection.Connection;
import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Blocks traffic whose TLS SNI matches configured domain patterns.
 * <p>
 * Supports exact matching and wildcard prefixes (e.g., {@code *.example.com}).
 * Packets without SNI are always allowed by this rule.
 * </p>
 */
@Getter
public class DomainBlockRule implements Rule {

    private final String name;
    private final Set<String> blockedPatterns;

    public DomainBlockRule(String name, Set<String> blockedPatterns) {
        this.name = name;
        this.blockedPatterns = Collections.unmodifiableSet(new HashSet<>(blockedPatterns));
    }

    @Override
    public Decision evaluate(ParsedPacket packet, Connection connection) {
        String sni = packet.getSni();
        if (sni == null || sni.isBlank()) {
            return Decision.ALLOW;
        }

        String lowerSni = sni.toLowerCase();

        for (String pattern : blockedPatterns) {
            String lowerPattern = pattern.toLowerCase();
            if (lowerPattern.startsWith("*.")) {
                // Wildcard: *.example.com matches foo.example.com
                String suffix = lowerPattern.substring(1); // ".example.com"
                if (lowerSni.endsWith(suffix) || lowerSni.equals(lowerPattern.substring(2))) {
                    return Decision.BLOCK;
                }
            } else if (lowerSni.equals(lowerPattern)) {
                return Decision.BLOCK;
            }
        }

        return Decision.ALLOW;
    }

    @Override
    public String getDescription() {
        return "Blocks domains matching: " + blockedPatterns;
    }
}
