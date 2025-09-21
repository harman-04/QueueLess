package com.queueless.backend.service;

import com.queueless.backend.dto.*;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Service;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.ServiceRepository;
import com.queueless.backend.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SearchService {

    private final PlaceRepository placeRepository;
    private final ServiceRepository serviceRepository;
    private final QueueRepository queueRepository;
    private final PlaceService placeService;
    private final QueueService queueService;
    private final MongoTemplate mongoTemplate;

    public SearchResultDTO comprehensiveSearch(SearchRequestDTO request, Pageable pageable) {
        log.info("Performing comprehensive search with filters: {}", request);

        SearchResultDTO result = new SearchResultDTO();

        // 1. Initialize a list to hold all relevant place IDs.
        List<String> placeIds = new ArrayList<>();

        // 2. Search for places first if the flag is true or if a place-specific filter is used.
        if (request.isSearchPlaces()) {
            Query placeQuery = new Query();
            List<Criteria> placeCriteriaList = new ArrayList<>();

            if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                placeCriteriaList.add(Criteria.where("name").regex(request.getQuery(), "i"));
            }
            if (request.getPlaceType() != null && !request.getPlaceType().isEmpty()) {
                placeCriteriaList.add(Criteria.where("type").is(request.getPlaceType()));
            }
            if (request.getMinRating() != null && request.getMinRating() > 0) {
                placeCriteriaList.add(Criteria.where("rating").gte(request.getMinRating()));
            }
            if (request.getIsActive() != null) {
                placeCriteriaList.add(Criteria.where("isActive").is(request.getIsActive()));
            }

            if (!placeCriteriaList.isEmpty()) {
                placeQuery.addCriteria(new Criteria().andOperator(placeCriteriaList.toArray(new Criteria[0])));
            }

            // Execute the place search and get the results
            List<Place> places = mongoTemplate.find(placeQuery.with(pageable), Place.class);
            long totalPlaces = mongoTemplate.count(Query.of(placeQuery).with(Pageable.unpaged()), Place.class);

            result.setPlaces(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
            result.setTotalPlaces(totalPlaces);

            // Collect place IDs for the next searches
            placeIds = places.stream().map(Place::getId).collect(Collectors.toList());
        }


        // 3. Search Services
        if (request.isSearchServices()) {
            Query serviceQuery = new Query();
            List<Criteria> serviceCriteriaList = new ArrayList<>();

            // If a place search was performed, use the returned place IDs as a filter.
            // Otherwise, search for services by name.
            if (!placeIds.isEmpty()) {
                serviceCriteriaList.add(Criteria.where("placeId").in(placeIds));
            } else if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                // If no places were found/searched, search services by name and description
                serviceCriteriaList.add(new Criteria().orOperator(
                        Criteria.where("name").regex(request.getQuery(), "i"),
                        Criteria.where("description").regex(request.getQuery(), "i")
                ));
            }

            // Add other filters if they are present
            if (request.getSupportsGroupToken() != null) {
                serviceCriteriaList.add(Criteria.where("supportsGroupToken").is(request.getSupportsGroupToken()));
            }
            if (request.getEmergencySupport() != null) {
                serviceCriteriaList.add(Criteria.where("emergencySupport").is(request.getEmergencySupport()));
            }
            if (request.getIsActive() != null) {
                serviceCriteriaList.add(Criteria.where("isActive").is(request.getIsActive()));
            }

            if (!serviceCriteriaList.isEmpty()) {
                serviceQuery.addCriteria(new Criteria().andOperator(serviceCriteriaList.toArray(new Criteria[0])));
            }

            List<Service> services = mongoTemplate.find(serviceQuery.with(pageable), Service.class);
            long totalServices = mongoTemplate.count(Query.of(serviceQuery).with(Pageable.unpaged()), Service.class);

            result.setServices(services.stream().map(ServiceDTO::fromEntity).collect(Collectors.toList()));
            result.setTotalServices(totalServices);
        }

        // 4. Search Queues
        if (request.isSearchQueues()) {
            Query queueQuery = new Query();
            List<Criteria> queueCriteriaList = new ArrayList<>();

            // If a place search was performed, use the returned place IDs as a filter.
            // Otherwise, search for queues by serviceName.
            if (!placeIds.isEmpty()) {
                queueCriteriaList.add(Criteria.where("placeId").in(placeIds));
            } else if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                queueCriteriaList.add(Criteria.where("serviceName").regex(request.getQuery(), "i"));
            }

            // Add other filters if they are present
            if (request.getMaxWaitTime() != null) {
                queueCriteriaList.add(Criteria.where("estimatedWaitTime").lte(request.getMaxWaitTime()));
            }
            if (request.getSupportsGroupToken() != null) {
                queueCriteriaList.add(Criteria.where("supportsGroupToken").is(request.getSupportsGroupToken()));
            }
            if (request.getEmergencySupport() != null) {
                queueCriteriaList.add(Criteria.where("emergencySupport").is(request.getEmergencySupport()));
            }
            if (request.getIsActive() != null) {
                queueCriteriaList.add(Criteria.where("isActive").is(request.getIsActive()));
            }

            if (!queueCriteriaList.isEmpty()) {
                queueQuery.addCriteria(new Criteria().andOperator(queueCriteriaList.toArray(new Criteria[0])));
            }

            List<Queue> queues = mongoTemplate.find(queueQuery.with(pageable), Queue.class);
            long totalQueues = mongoTemplate.count(Query.of(queueQuery).with(Pageable.unpaged()), Queue.class);

            // Fetch place information for the found queues
            List<String> queuePlaceIds = queues.stream()
                    .map(Queue::getPlaceId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, Place> placeMap = placeService.getPlacesByIds(queuePlaceIds).stream()
                    .collect(Collectors.toMap(Place::getId, Function.identity()));

            List<EnhancedQueueDTO> enhancedQueues = queues.stream()
                    .map(queue -> {
                        EnhancedQueueDTO dto = EnhancedQueueDTO.fromQueue(queue);
                        dto.setCurrentWaitTime(queueService.calculateCurrentWaitTime(queue.getId()));
                        Place place = placeMap.get(queue.getPlaceId());
                        if (place != null) {
                            dto.setPlaceName(place.getName());
                            dto.setPlaceAddress(place.getAddress());
                            dto.setPlaceRating(place.getRating());
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());

            result.setQueues(enhancedQueues);
            result.setTotalQueues(totalQueues);
        }

        return result;
    }

    // ... (other methods remain the same)
    public List<Place> searchNearbyWithFilters(Double longitude, Double latitude, Double radius,
                                               SearchRequestDTO request) {
        return placeRepository.searchNearbyWithFilters(
                request.getQuery(),
                request.getPlaceTypes(),
                request.getMinRating(),
                longitude,
                latitude,
                radius * 1000 // Convert km to meters
        );
    }

    public List<String> getAvailablePlaceTypes() {
        return placeRepository.findDistinctTypes().stream()
                .map(Place::getType)
                .distinct()
                .collect(Collectors.toList());
    }

    public SearchStatisticsDTO getSearchStatistics(SearchRequestDTO request) {
        SearchStatisticsDTO statistics = new SearchStatisticsDTO();

        Long placeCount = placeRepository.countByFilters(
                request.getQuery(),
                request.getPlaceType(),
                request.getMinRating(),
                request.getIsActive()
        );
        statistics.setTotalPlaces(placeCount);

        return statistics;
    }
}