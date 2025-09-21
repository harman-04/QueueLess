package com.queueless.backend.controller;

import com.queueless.backend.dto.*;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/comprehensive")
    public ResponseEntity<SearchResultDTO> comprehensiveSearch(
            @Valid @RequestBody SearchRequestDTO request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        SearchResultDTO result = searchService.comprehensiveSearch(request, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/nearby")
    public ResponseEntity<List<PlaceDTO>> searchNearby(
            @Valid @RequestBody SearchRequestDTO request) {

        if (request.getLongitude() == null || request.getLatitude() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Place> places = searchService.searchNearbyWithFilters(
                request.getLongitude(),
                request.getLatitude(),
                request.getRadius(),
                request
        );

        List<PlaceDTO> result = places.stream()
                .map(PlaceDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> options = new HashMap<>();

        options.put("placeTypes", searchService.getAvailablePlaceTypes());
        options.put("serviceTypes", Arrays.asList("MEDICAL", "RETAIL", "FOOD", "SERVICE", "OTHER"));
        options.put("waitTimeRanges", Arrays.asList(
                Map.of("label", "Under 15 min", "value", 15),
                Map.of("label", "15-30 min", "value", 30),
                Map.of("label", "30-60 min", "value", 60),
                Map.of("label", "Over 60 min", "value", 120)
        ));

        return ResponseEntity.ok(options);
    }

    @PostMapping("/statistics")
    public ResponseEntity<SearchStatisticsDTO> getSearchStatistics(
            @Valid @RequestBody SearchRequestDTO request) {

        SearchStatisticsDTO statistics = searchService.getSearchStatistics(request);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/quick/{query}")
    public ResponseEntity<SearchResultDTO> quickSearch(
            @PathVariable String query,
            @RequestParam(defaultValue = "5") int limit) {

        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(query);
        request.setSearchServices(true);
        request.setSearchPlaces(true);
        request.setSearchQueues(true);

        Pageable pageable = PageRequest.of(0, limit);
        SearchResultDTO result = searchService.comprehensiveSearch(request, pageable);

        return ResponseEntity.ok(result);
    }
}