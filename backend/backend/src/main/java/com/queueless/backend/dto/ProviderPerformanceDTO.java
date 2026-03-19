
package com.queueless.backend.dto;

import com.queueless.backend.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Provider performance statistics")
public class ProviderPerformanceDTO {
    @Schema(description = "Provider ID", example = "507f1f77bcf86cd799439014")
    private String id;

    @Schema(description = "Provider name", example = "Dr. Smith")
    private String name;

    @Schema(description = "Provider email", example = "smith@clinic.com")
    private String email;

    @Schema(description = "Total number of queues managed", example = "3")
    private int totalQueues;

    @Schema(description = "Number of active queues", example = "2")
    private int activeQueues;

    @Schema(description = "Number of tokens served today", example = "15")
    private long tokensServedToday;

    @Schema(description = "Average rating received", example = "4.5")
    private double averageRating;

    @Schema(description = "Cancellation rate (%)", example = "2.5")
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