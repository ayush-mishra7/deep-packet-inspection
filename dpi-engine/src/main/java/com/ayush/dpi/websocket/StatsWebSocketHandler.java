package com.ayush.dpi.websocket;

import com.ayush.dpi.stats.StatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Broadcasts DPI pipeline statistics to all connected WebSocket clients.
 */
@Slf4j
@Component
public class StatsWebSocketHandler extends TextWebSocketHandler {

    private final StatsService statsService;
    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private long lastTotalPackets = 0;
    private long lastBroadcastTime = System.currentTimeMillis();

    public StatsWebSocketHandler(StatsService statsService, ObjectMapper objectMapper) {
        this.statsService = statsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket client disconnected: {}", session.getId());
    }

    /**
     * Broadcast live stats every 1 second to all connected clients.
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastStats() {
        if (sessions.isEmpty()) {
            return; // Don't build payload if nobody is listening
        }

        try {
            long currentTotalPackets = statsService.getTotalPackets();
            long currentTime = System.currentTimeMillis();
            long timeDiffMs = currentTime - lastBroadcastTime;

            long throughputPerSecond = 0;
            if (timeDiffMs > 0) {
                throughputPerSecond = (long) ((currentTotalPackets - lastTotalPackets) * 1000.0 / timeDiffMs);
            }

            lastTotalPackets = currentTotalPackets;
            lastBroadcastTime = currentTime;

            long tcp = statsService.getTcpPackets();
            long udp = statsService.getUdpPackets();
            long totalProtocols = tcp + udp;
            double tcpRatio = totalProtocols == 0 ? 0.0 : (double) tcp / totalProtocols;
            double udpRatio = totalProtocols == 0 ? 0.0 : (double) udp / totalProtocols;

            Map<String, Long> topDomains = statsService.getDomainFrequency().entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
                    .limit(10)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum(), (e1, e2) -> e1,
                            LinkedHashMap::new));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPackets", currentTotalPackets);
            stats.put("throughputPerSecond", throughputPerSecond);
            stats.put("tcpRatio", tcpRatio);
            stats.put("udpRatio", udpRatio);
            stats.put("allowedCount", statsService.getAllowedPackets());
            stats.put("blockedCount", statsService.getBlockedPackets());
            stats.put("throttledCount", statsService.getThrottledPackets());
            stats.put("topDomains", topDomains);

            String message = objectMapper.writeValueAsString(stats);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            log.error("Error broadcasting WebSocket stats", e);
        }
    }
}
