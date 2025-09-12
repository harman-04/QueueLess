// FeedbackDTO.java
package com.queueless.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class FeedbackDTO {
    @NotNull
    private String tokenId;

    @NotNull
    private String queueId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 500)
    private String comment;

    @Min(1)
    @Max(5)
    private Integer staffRating;

    @Min(1)
    @Max(5)
    private Integer serviceRating;

    @Min(1)
    @Max(5)
    private Integer waitTimeRating;
}