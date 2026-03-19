package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Enhanced queue information for admin dashboard")
public class AdminQueueDTO {
    @Schema(description = "Queue ID", example = "queue123")
    private String id;

    @Schema(description = "Service name", example = "General Consultation")
    private String serviceName;

    @Schema(description = "Place name", example = "Central Hospital")
    private String placeName;

    @Schema(description = "Provider name", example = "Dr. Smith")
    private String providerName;

    @Schema(description = "Whether the queue is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Number of waiting tokens", example = "5")
    private Integer waitingTokens;

    @Schema(description = "Number of tokens currently in service", example = "1")
    private Integer inServiceTokens;

    @Schema(description = "Number of completed tokens", example = "42")
    private Integer completedTokens;

    @Schema(description = "Estimated wait time in minutes", example = "25")
    private Integer estimatedWaitTime;
}