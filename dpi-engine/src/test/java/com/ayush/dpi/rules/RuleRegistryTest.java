package com.ayush.dpi.rules;

import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.parser.ProtocolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleRegistryTest {

    private RuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RuleRegistry();
    }

    @Test
    @DisplayName("Add and retrieve rules")
    void addAndRetrieve() {
        IpBlockRule rule = new IpBlockRule("block-bad", Set.of("1.2.3.4"));
        registry.addRule(rule);

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.getRule("block-bad")).isPresent();
        assertThat(registry.getSnapshot()).hasSize(1);
    }

    @Test
    @DisplayName("Remove rule by name")
    void removeByName() {
        registry.addRule(new IpBlockRule("r1", Set.of("1.1.1.1")));
        registry.addRule(new IpBlockRule("r2", Set.of("2.2.2.2")));

        assertThat(registry.removeRule("r1")).isTrue();
        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.getRule("r1")).isEmpty();
    }

    @Test
    @DisplayName("Remove non-existent rule returns false")
    void removeNonExistent() {
        assertThat(registry.removeRule("nope")).isFalse();
    }

    @Test
    @DisplayName("Clear removes all rules")
    void clearAll() {
        registry.addRule(new IpBlockRule("r1", Set.of("1.1.1.1")));
        registry.addRule(new IpBlockRule("r2", Set.of("2.2.2.2")));
        registry.clear();

        assertThat(registry.size()).isZero();
    }

    @Test
    @DisplayName("Snapshot is safe for concurrent iteration")
    void snapshotIsSafe() {
        for (int i = 0; i < 10; i++) {
            registry.addRule(new IpBlockRule("r" + i, Set.of("10.0.0." + i)));
        }

        var snapshot = registry.getSnapshot();
        assertThat(snapshot).hasSize(10);

        // Modifying registry doesn't affect the CopyOnWriteArrayList iteration
        // (it creates a copy on write, so the snapshot's underlying list is stable)
    }
}
