package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "User queue restriction status")
public class UserQueueRestrictionDTO {
    @Schema(description = "User ID", example = "507f1f77bcf86cd799439015")
    private String userId;

    @Schema(description = "Whether user can join a new queue", example = "true")
    private boolean canJoinQueue;

    @Schema(description = "Last time user joined a queue")
    private LocalDateTime lastJoinTime;

    @Schema(description = "Time after which user can join again")
    private LocalDateTime canJoinAfter;

    @Schema(description = "Reason if joining is restricted", example = "You already have an active token...")
    private String restrictionReason;
}