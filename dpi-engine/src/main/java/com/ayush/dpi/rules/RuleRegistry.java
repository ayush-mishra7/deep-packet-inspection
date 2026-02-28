package com.ayush.dpi.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry of active rules.
 * <p>
 * Uses {@link CopyOnWriteArrayList} so workers can iterate over rules
 * without synchronization while the REST API can add/remove rules atomically.
 * </p>
 */
@Slf4j
@Service
public class RuleRegistry {

    private final CopyOnWriteArrayList<Rule> rules = new CopyOnWriteArrayList<>();

    /**
     * Add a rule to the registry.
     *
     * @param rule the rule to add
     */
    public void addRule(Rule rule) {
        rules.add(rule);
        log.info("Rule added: [{}] — {}", rule.getName(), rule.getDescription());
    }

    /**
     * Remove a rule by name.
     *
     * @param name the rule name
     * @return true if a rule was removed
     */
    public boolean removeRule(String name) {
        boolean removed = rules.removeIf(r -> r.getName().equals(name));
        if (removed) {
            log.info("Rule removed: [{}]", name);
        }
        return removed;
    }

    /**
     * Get a rule by name.
     *
     * @param name the rule name
     * @return the rule if found
     */
    public Optional<Rule> getRule(String name) {
        return rules.stream().filter(r -> r.getName().equals(name)).findFirst();
    }

    /**
     * @return unmodifiable snapshot of all active rules (safe for iteration)
     */
    public List<Rule> getSnapshot() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * @return current number of active rules
     */
    public int size() {
        return rules.size();
    }

    /**
     * Remove all rules.
     */
    public void clear() {
        rules.clear();
        log.info("All rules cleared");
    }
}
