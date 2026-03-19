package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated search statistics")
public class SearchStatisticsDTO {
    @Schema(description = "Total number of places")
    private Long totalPlaces;

    @Schema(description = "Total number of services")
    private Long totalServices;

    @Schema(description = "Total number of queues")
    private Long totalQueues;

    @Schema(description = "Total number of active queues")
    private Long totalActiveQueues;

    @Schema(description = "Average wait time across queues (minutes)")
    private Double averageWaitTime;

    @Schema(description = "Average rating across places")
    private Double averageRating;
}