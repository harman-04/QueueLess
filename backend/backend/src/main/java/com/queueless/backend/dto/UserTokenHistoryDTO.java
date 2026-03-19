package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User's token history entry")
public class UserTokenHistoryDTO {
    @Schema(description = "Token ID", example = "queue123-T-001")
    private String tokenId;

    @Schema(description = "Queue ID", example = "67b1a2c3d4e5f67890123457")
    private String queueId;

    @Schema(description = "Service name", example = "General Consultation")
    private String serviceName;

    @Schema(description = "Place ID", example = "67b1a2c3d4e5f67890123456")
    private String placeId;

    @Schema(description = "Place name", example = "Central Hospital")
    private String placeName;

    @Schema(description = "Token status (COMPLETED/CANCELLED)", example = "COMPLETED")
    private String status;

    @Schema(description = "Timestamp when token was issued")
    private LocalDateTime issuedAt;

    @Schema(description = "Timestamp when service started")
    private LocalDateTime servedAt;

    @Schema(description = "Timestamp when service completed")
    private LocalDateTime completedAt;

    @Schema(description = "Wait time in minutes (issued → served)", example = "15")
    private Long waitTimeMinutes;

    @Schema(description = "Service duration in minutes (served → completed)", example = "10")
    private Long serviceDurationMinutes;

    @Schema(description = "Rating given by user (1-5)", example = "4")
    private Integer rating;
}