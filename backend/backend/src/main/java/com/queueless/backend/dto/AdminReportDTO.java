package com.queueless.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportDTO {
    private String adminName;
    private String adminEmail;
    private LocalDateTime generatedAt;
    private List<PlaceSummaryDTO> places;
    private GlobalSummaryDTO summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceSummaryDTO {
        private String placeId;
        private String placeName;
        private int totalQueues;
        private int activeQueues;
        private long tokensServedToday;
        private long tokensServedTotal;
        private double averageWaitTime;
        private double averageRating;
        private long activeTokens; // waiting + in-service
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalSummaryDTO {
        private int totalPlaces;
        private int totalQueues;
        private long totalTokensServedToday;
        private long totalTokensServedAllTime;
        private double averageRatingOverall;
        private double averageWaitTimeOverall;
    }
}