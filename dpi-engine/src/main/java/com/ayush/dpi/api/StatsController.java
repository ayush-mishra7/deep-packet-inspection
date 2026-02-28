package com.ayush.dpi.api;

import com.ayush.dpi.api.dto.StatsResponse;
import com.ayush.dpi.stats.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint exposing real-time aggregated DPI statistics.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

        private final StatsService statsService;

        @GetMapping
        public ResponseEntity<StatsResponse> getGlobalStats() {
                long total = statsService.getTotalPackets();
                long tcp = statsService.getTcpPackets();
                long udp = statsService.getUdpPackets();
                long other = total - tcp - udp;

                // Extract top 10 domains by frequency
                List<StatsResponse.DomainStat> topDomains = statsService.getDomainFrequency().entrySet()
                                .stream()
                                .map(e -> StatsResponse.DomainStat.builder()
                                                .domain(e.getKey())
                                                .count(e.getValue().sum())
                                                .build())
                                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount())) // Descending
                                .limit(10)
                                .toList();

                StatsResponse response = StatsResponse.builder()
                                .overall(StatsResponse.OverallStats.builder()
                                                .totalPacketsProcessed(total)
                                                .totalBytesProcessed(statsService.getTotalBytes())
                                                .build())
                                .protocols(StatsResponse.ProtocolStats.builder()
                                                .tcpPackets(tcp)
                                                .udpPackets(udp)
                                                .otherPackets(Math.max(0, other))
                                                .build())
                                .decisions(StatsResponse.DecisionStats.builder()
                                                .allowed(statsService.getAllowedPackets())
                                                .blocked(statsService.getBlockedPackets())
                                                .throttled(statsService.getThrottledPackets())
                                                .build())
                                .topDomains(topDomains)
                                .build();

                return ResponseEntity.ok(response);
        }
}
