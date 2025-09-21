package com.queueless.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchStatisticsDTO {
    private Long totalPlaces;
    private Long totalServices;
    private Long totalQueues;
    private Long totalActiveQueues;
    private Double averageWaitTime;
    private Double averageRating;
}