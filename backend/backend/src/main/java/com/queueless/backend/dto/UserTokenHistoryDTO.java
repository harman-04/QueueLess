package com.queueless.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenHistoryDTO {
    private String tokenId;
    private String queueId;
    private String serviceName;
    private String placeId;          // added for batch lookup
    private String placeName;         // will be set later
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime servedAt;
    private LocalDateTime completedAt;
    private Long waitTimeMinutes;
    private Long serviceDurationMinutes;
    private Integer rating;
}