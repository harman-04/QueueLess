package com.queueless.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search results with pagination metadata")
public class SearchResultDTO {
    @Schema(description = "List of places found")
    private List<PlaceDTO> places;

    @Schema(description = "List of services found")
    private List<ServiceDTO> services;

    @Schema(description = "List of enhanced queues found")
    private List<EnhancedQueueDTO> queues;

    @Schema(description = "Total number of places matching the query")
    private Long totalPlaces;

    @Schema(description = "Total number of services matching the query")
    private Long totalServices;

    @Schema(description = "Total number of queues matching the query")
    private Long totalQueues;

    @Schema(description = "Current page number for places (0‑based)")
    private Integer placesPage;

    @Schema(description = "Total pages for places")
    private Integer placesTotalPages;

    @Schema(description = "Current page number for services (0‑based)")
    private Integer servicesPage;

    @Schema(description = "Total pages for services")
    private Integer servicesTotalPages;

    @Schema(description = "Current page number for queues (0‑based)")
    private Integer queuesPage;

    @Schema(description = "Total pages for queues")
    private Integer queuesTotalPages;

    @Schema(description = "Search statistics")
    private SearchStatisticsDTO statistics;
}