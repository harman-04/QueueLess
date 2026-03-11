package com.queueless.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPositionDTO {
    private String queueId;
    private String userId;
    private String tokenId;
    private Integer position;          // 1‑based position, null if not in queue
    private String status;             // WAITING, IN_SERVICE, etc.
    private Integer estimatedWaitTime; // in minutes
}