package com.ayush.dpi.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for rule match events and persists them to the PostgreSQL database
 * asynchronously
 * without blocking the hot packet processing threads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final RuleAuditLogRepository repository;

    @Async
    @EventListener
    public void handleRuleMatchEvent(AuditEventPublisher.RuleMatchEvent event) {
        try {
            RuleAuditLogEntity entity = RuleAuditLogEntity.builder()
                    .timestamp(event.getEventTime())
                    .srcIp(event.getPacket().getSrcIp())
                    .destIp(event.getPacket().getDestIp())
                    .destPort(event.getPacket().getDestPort())
                    .decision(event.getDecision())
                    .ruleName(event.getRule().getName())
                    .matchedSni(event.getPacket().getSni())
                    .build();

            repository.save(entity);
            log.trace("Persisted audit log for packet seq: {}", event.getPacket().getSequenceNumber());
        } catch (Exception e) {
            log.error("Failed to asynchronously persist rule audit log: {}", e.getMessage(), e);
        }
    }
}
