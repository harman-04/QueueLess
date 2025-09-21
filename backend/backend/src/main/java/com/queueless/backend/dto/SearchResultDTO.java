package com.queueless.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private List<PlaceDTO> places;
    private List<ServiceDTO> services;
    private List<EnhancedQueueDTO> queues;

    private Long totalPlaces;
    private Long totalServices;
    private Long totalQueues;

    private SearchStatisticsDTO statistics;
}


