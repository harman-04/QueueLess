// src/main/java/com/queueless/backend/dto/ProviderPerformanceDTO.java
package com.queueless.backend.dto;

import com.queueless.backend.model.User;
import lombok.Data;

@Data
public class ProviderPerformanceDTO {
    private String id;
    private String name;
    private String email;
    private int totalQueues;
    private int activeQueues;
    private long tokensServedToday;
    private double averageRating;
    private double cancellationRate;

    public ProviderPerformanceDTO(User provider, int totalQueues, int activeQueues,
                                  long tokensServedToday, double averageRating, double cancellationRate) {
        this.id = provider.getId();
        this.name = provider.getName();
        this.email = provider.getEmail();
        this.totalQueues = totalQueues;
        this.activeQueues = activeQueues;
        this.tokensServedToday = tokensServedToday;
        this.averageRating = averageRating;
        this.cancellationRate = cancellationRate;
    }
}