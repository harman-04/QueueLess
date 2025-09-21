package com.queueless.backend.dto;

import com.queueless.backend.model.Queue;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedQueueDTO {
    private String id;
    private String serviceName;
    private String placeId;
    private String placeName;
    private String placeAddress;
    private Double placeRating;
    private Boolean isActive;
    private Integer estimatedWaitTime;
    private Integer currentWaitTime;
    private Integer waitingTokens;
    private Boolean supportsGroupToken;
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