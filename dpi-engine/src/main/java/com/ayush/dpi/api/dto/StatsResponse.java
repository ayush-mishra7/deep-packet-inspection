package com.ayush.dpi.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO containing aggregated real-time DPI statistics.
 */
@Getter
@Builder
public class StatsResponse {

    private final OverallStats overall;
    private final ProtocolStats protocols;
    private final DecisionStats decisions;
    private final List<DomainStat> topDomains;

    @Getter
    @Builder
    public static class OverallStats {
        private final long totalPacketsProcessed;
        private final long totalBytesProcessed;
    }

    @Getter
    @Builder
    public static class ProtocolStats {
        private final long tcpPackets;
        private final long udpPackets;
        private final long otherPackets;
    }

    @Getter
    @Builder
    public static class DecisionStats {
        private final long allowed;
        private final long blocked;
        private final long throttled;
    }

    @Getter
    @Builder
    public static class DomainStat {
        private final String domain;
        private final long count;
    }
}
