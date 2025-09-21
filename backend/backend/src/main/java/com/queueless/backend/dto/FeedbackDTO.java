package com.queueless.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class FeedbackDTO {
    @NotNull(message = "Token ID is required")
    private String tokenId;

    @NotNull(message = "Queue ID is required")
    private String queueId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;

    @Min(value = 1, message = "Staff rating must be between 1 and 5")
    @Max(value = 5, message = "Staff rating must be between 1 and 5")
    private Integer staffRating;

    @Min(value = 1, message = "Service rating must be between 1 and 5")
    @Max(value = 5, message = "Service rating must be between 1 and 5")
    private Integer serviceRating;

    @Min(value = 1, message = "Wait time rating must be between 1 and 5")
    @Max(value = 5, message = "Wait time rating must be between 1 and 5")
    private Integer waitTimeRating;
}