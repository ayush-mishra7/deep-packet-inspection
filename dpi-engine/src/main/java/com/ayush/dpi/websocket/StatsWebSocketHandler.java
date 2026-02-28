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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts DPI pipeline statistics to all connected WebSocket clients.
 */
@Slf4j
@Component
public class StatsWebSocketHandler extends TextWebSocketHandler {

    private final StatsService statsService;
    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

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
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPackets", statsService.getTotalPackets());
            stats.put("totalBytes", statsService.getTotalBytes());
            stats.put("tcpPackets", statsService.getTcpPackets());
            stats.put("udpPackets", statsService.getUdpPackets());
            stats.put("allowedPackets", statsService.getAllowedPackets());
            stats.put("blockedPackets", statsService.getBlockedPackets());
            stats.put("throttledPackets", statsService.getThrottledPackets());
            stats.put("errorPackets", statsService.getErrorPackets());

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
