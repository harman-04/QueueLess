package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User's current position in a queue")
public class UserPositionDTO {
    @Schema(description = "Queue ID", example = "queue123")
    private String queueId;

    @Schema(description = "User ID", example = "user123")
    private String userId;

    @Schema(description = "Token ID (if user has a token)", example = "queue123-T-001")
    private String tokenId;

    @Schema(description = "1‑based position in waiting list", example = "3")
    private Integer position;

    @Schema(description = "Current status of the token", example = "WAITING")
    private String status;

    @Schema(description = "Estimated wait time in minutes", example = "15")
    private Integer estimatedWaitTime;
}