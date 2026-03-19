package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Admin report containing summary and per‑place details")
public class AdminReportDTO {

    @Schema(description = "Admin's name", example = "John Doe")
    private String adminName;

    @Schema(description = "Admin's email", example = "admin@example.com")
    private String adminEmail;

    @Schema(description = "Report generation timestamp")
    private LocalDateTime generatedAt;

    @Schema(description = "List of places with their statistics")
    private List<PlaceSummaryDTO> places;

    @Schema(description = "Global summary across all places")
    private GlobalSummaryDTO summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary for a single place")
    public static class PlaceSummaryDTO {
        @Schema(description = "Place ID", example = "67b1a2c3d4e5f67890123456")
        private String placeId;

        @Schema(description = "Place name", example = "Central Hospital")
        private String placeName;

        @Schema(description = "Total number of queues under this place", example = "5")
        private int totalQueues;

        @Schema(description = "Number of active queues", example = "3")
        private int activeQueues;

        @Schema(description = "Tokens served today", example = "120")
        private long tokensServedToday;

        @Schema(description = "Total tokens served all time", example = "15420")
        private long tokensServedTotal;

        @Schema(description = "Average wait time (minutes)", example = "12.5")
        private double averageWaitTime;

        @Schema(description = "Average rating (1-5)", example = "4.2")
        private double averageRating;

        @Schema(description = "Active tokens (waiting + in‑service)", example = "8")
        private long activeTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Global summary statistics")
    public static class GlobalSummaryDTO {
        @Schema(description = "Total number of places", example = "3")
        private int totalPlaces;

        @Schema(description = "Total number of queues", example = "15")
        private int totalQueues;

        @Schema(description = "Total tokens served today", example = "350")
        private long totalTokensServedToday;

        @Schema(description = "Total tokens served all time", example = "45210")
        private long totalTokensServedAllTime;

        @Schema(description = "Average rating overall (1-5)", example = "4.3")
        private double averageRatingOverall;

        @Schema(description = "Average wait time overall (minutes)", example = "14.2")
        private double averageWaitTimeOverall;
    }
}