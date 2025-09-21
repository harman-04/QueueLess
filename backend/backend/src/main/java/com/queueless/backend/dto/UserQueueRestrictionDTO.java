package com.queueless.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserQueueRestrictionDTO {
    private String userId;
    private boolean canJoinQueue;
    private LocalDateTime lastJoinTime;
    private LocalDateTime canJoinAfter;
    private String restrictionReason;

    public UserQueueRestrictionDTO(String userId, boolean canJoinQueue,
                                   LocalDateTime lastJoinTime, LocalDateTime canJoinAfter,
                                   String restrictionReason) {
        this.userId = userId;
        this.canJoinQueue = canJoinQueue;
        this.lastJoinTime = lastJoinTime;
        this.canJoinAfter = canJoinAfter;
        this.restrictionReason = restrictionReason;
    }
}