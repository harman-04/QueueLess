package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Feedback submission")
public class FeedbackDTO {
    @NotNull(message = "Token ID is required")
    @Schema(description = "ID of the token being rated", example = "queue123-T-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenId;

    @NotNull(message = "Queue ID is required")
    @Schema(description = "ID of the queue", example = "queue123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queueId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Schema(description = "Overall rating (1‑5)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rating;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    @Schema(description = "Optional comment", example = "Great service, very fast")
    private String comment;

    @Min(value = 1, message = "Staff rating must be between 1 and 5")
    @Max(value = 5, message = "Staff rating must be between 1 and 5")
    @Schema(description = "Rating for staff (1‑5)", example = "5")
    private Integer staffRating;

    @Min(value = 1, message = "Service rating must be between 1 and 5")
    @Max(value = 5, message = "Service rating must be between 1 and 5")
    @Schema(description = "Rating for service quality (1‑5)", example = "4")
    private Integer serviceRating;

    @Min(value = 1, message = "Wait time rating must be between 1 and 5")
    @Max(value = 5, message = "Wait time rating must be between 1 and 5")
    @Schema(description = "Rating for wait time (1‑5)", example = "3")
    private Integer waitTimeRating;
}