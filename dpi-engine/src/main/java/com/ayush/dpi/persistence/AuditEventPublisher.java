package com.ayush.dpi.persistence;

import com.ayush.dpi.decision.Decision;
import com.ayush.dpi.parser.ParsedPacket;
import com.ayush.dpi.rules.Rule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Publishes audit events when a rule triggers a non-ALLOW decision.
 */
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishRuleMatch(ParsedPacket packet, Rule rule, Decision decision) {
        RuleMatchEvent event = new RuleMatchEvent(this, packet, rule, decision);
        publisher.publishEvent(event);
    }

    @Getter
    public static class RuleMatchEvent extends ApplicationEvent {
        private final ParsedPacket packet;
        private final Rule rule;
        private final Decision decision;
        private final Instant eventTime;

        public RuleMatchEvent(Object source, ParsedPacket packet, Rule rule, Decision decision) {
            super(source);
            this.packet = packet;
            this.rule = rule;
            this.decision = decision;
            this.eventTime = Instant.now();
        }
    }
}
