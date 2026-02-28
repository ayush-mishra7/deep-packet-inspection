package com.ayush.dpi.api;

import com.ayush.dpi.api.dto.RuleRequest;
import com.ayush.dpi.api.dto.RuleResponse;
import com.ayush.dpi.rules.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST endpoints for managing DPI traffic rules.
 */
@Slf4j
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleRegistry ruleRegistry;

    /**
     * Create a new rule.
     */
    @PostMapping
    public ResponseEntity<?> createRule(@Valid @RequestBody RuleRequest request) {
        if (ruleRegistry.getRule(request.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Rule with name '" + request.getName() + "' already exists"));
        }

        Rule rule;
        try {
            rule = buildRule(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }

        ruleRegistry.addRule(rule);

        RuleResponse response = RuleResponse.builder()
                .name(rule.getName())
                .type(request.getType().toUpperCase())
                .description(rule.getDescription())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all active rules.
     */
    @GetMapping
    public ResponseEntity<List<RuleResponse>> listRules() {
        List<RuleResponse> rules = ruleRegistry.getSnapshot().stream()
                .map(r -> RuleResponse.builder()
                        .name(r.getName())
                        .type(r.getClass().getSimpleName())
                        .description(r.getDescription())
                        .build())
                .toList();
        return ResponseEntity.ok(rules);
    }

    /**
     * Delete a rule by name.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<?> deleteRule(@PathVariable String name) {
        boolean removed = ruleRegistry.removeRule(name);
        if (removed) {
            return ResponseEntity.ok(Map.of("message", "Rule '" + name + "' removed"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Rule '" + name + "' not found"));
    }

    private Rule buildRule(RuleRequest request) {
        String type = request.getType().toUpperCase();
        return switch (type) {
            case "IP_BLOCK" -> {
                validateValues(request);
                yield new IpBlockRule(request.getName(), request.getValues());
            }
            case "DOMAIN_BLOCK" -> {
                validateValues(request);
                yield new DomainBlockRule(request.getName(), request.getValues());
            }
            case "DATA_CAP" -> {
                if (request.getThreshold() == null || request.getThreshold() <= 0) {
                    throw new IllegalArgumentException("DATA_CAP requires a positive 'threshold' value");
                }
                yield new DataCapRule(request.getName(), request.getThreshold());
            }
            default -> throw new IllegalArgumentException(
                    "Unknown rule type: " + type + ". Valid: IP_BLOCK, DOMAIN_BLOCK, DATA_CAP");
        };
    }

    private void validateValues(RuleRequest request) {
        Set<String> values = request.getValues();
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(request.getType() + " requires non-empty 'values'");
        }
    }
}
