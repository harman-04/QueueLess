package com.queueless.backend.dto;

import com.queueless.backend.model.Queue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Enhanced queue information with place details")
public class EnhancedQueueDTO {
    @Schema(description = "Queue ID", example = "67b1a2c3d4e5f67890123457")
    private String id;

    @Schema(description = "Service name", example = "General Consultation")
    private String serviceName;

    @Schema(description = "Place ID", example = "67b1a2c3d4e5f67890123456")
    private String placeId;

    @Schema(description = "Place name", example = "Central Hospital")
    private String placeName;

    @Schema(description = "Place address", example = "123 Main St, City")
    private String placeAddress;

    @Schema(description = "Place rating", example = "4.5")
    private Double placeRating;

    @Schema(description = "Whether the queue is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Estimated wait time (minutes)", example = "15")
    private Integer estimatedWaitTime;

    @Schema(description = "Current wait time (calculated)", example = "12")
    private Integer currentWaitTime;

    @Schema(description = "Number of waiting tokens", example = "5")
    private Integer waitingTokens;

    @Schema(description = "Whether group tokens are supported", example = "true")
    private Boolean supportsGroupToken;

    @Schema(description = "Whether emergency tokens are supported", example = "false")
    private Boolean emergencySupport;

    public static EnhancedQueueDTO fromQueue(Queue queue) {
        EnhancedQueueDTO dto = new EnhancedQueueDTO();
        dto.setId(queue.getId());
        dto.setServiceName(queue.getServiceName());
        dto.setPlaceId(queue.getPlaceId());
        dto.setIsActive(queue.getIsActive());
        dto.setEstimatedWaitTime(queue.getEstimatedWaitTime());
        dto.setSupportsGroupToken(queue.getSupportsGroupToken());
        dto.setEmergencySupport(queue.getEmergencySupport());

        // Calculate waiting tokens
        if (queue.getTokens() != null) {
            long waitingCount = queue.getTokens().stream()
                    .filter(token -> "WAITING".equals(token.getStatus()))
                    .count();
            dto.setWaitingTokens((int) waitingCount);
        } else {
            dto.setWaitingTokens(0);
        }

        return dto;
    }
}