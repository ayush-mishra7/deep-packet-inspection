package com.ayush.dpi.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO representing an active rule.
 */
@Getter
@Builder
@AllArgsConstructor
public class RuleResponse {
    private final String name;
    private final String type;
    private final String description;
}
