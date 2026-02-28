package com.ayush.dpi.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Request DTO for creating a new rule.
 */
@Getter
@Setter
public class RuleRequest {

    /** Rule type: IP_BLOCK, DOMAIN_BLOCK, or DATA_CAP */
    @NotBlank(message = "Rule type is required")
    private String type;

    /** Unique name for the rule */
    @NotBlank(message = "Rule name is required")
    private String name;

    /** IP addresses or domain patterns (used by IP_BLOCK and DOMAIN_BLOCK) */
    private Set<String> values;

    /** Byte threshold (used by DATA_CAP) */
    private Long threshold;
}
