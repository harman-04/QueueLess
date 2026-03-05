package com.queueless.backend.controller;

import com.queueless.backend.dto.*;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Search", description = "Endpoints for searching places, services, and queues")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/comprehensive")
    @Operation(summary = "Comprehensive search", description = "Performs a search across places, services, and queues with filters and pagination.")
    @ApiResponse(responseCode = "200", description = "Search results",
            content = @Content(schema = @Schema(implementation = SearchResultDTO.class)))
    public ResponseEntity<SearchResultDTO> comprehensiveSearch(
            @Valid @RequestBody SearchRequestDTO request,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("Comprehensive search request: query={}, page={}, size={}", request.getQuery(), page, size);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        SearchResultDTO result = searchService.comprehensiveSearch(request, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/nearby")
    @Operation(summary = "Nearby search", description = "Searches for places near a given location with filters.")
    @ApiResponse(responseCode = "200", description = "List of nearby places")
    @ApiResponse(responseCode = "400", description = "Missing longitude/latitude")
    public ResponseEntity<List<PlaceDTO>> searchNearby(
            @Valid @RequestBody SearchRequestDTO request) {

        if (request.getLongitude() == null || request.getLatitude() == null) {
            log.warn("Nearby search called without longitude/latitude");
            return ResponseEntity.badRequest().build();
        }

        log.info("Nearby search request: lon={}, lat={}, radius={}", request.getLongitude(), request.getLatitude(), request.getRadius());
        List<Place> places = searchService.searchNearbyWithFilters(
                request.getLongitude(),
                request.getLatitude(),
                request.getRadius(),
                request
        );

        List<PlaceDTO> result = places.stream()
                .map(PlaceDTO::fromEntity)
                .collect(Collectors.toList());

        log.info("Found {} nearby places", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/filter-options")
    @Operation(summary = "Get filter options", description = "Returns available filter values (place types, wait time ranges, etc.)")
    @ApiResponse(responseCode = "200", description = "Filter options map")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        log.debug("Fetching filter options");
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
    @Operation(summary = "Get search statistics", description = "Returns statistics about the search results (e.g., total places).")
    @ApiResponse(responseCode = "200", description = "Search statistics")
    public ResponseEntity<SearchStatisticsDTO> getSearchStatistics(
            @Valid @RequestBody SearchRequestDTO request) {

        log.info("Search statistics request for query: {}", request.getQuery());
        SearchStatisticsDTO statistics = searchService.getSearchStatistics(request);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/quick/{query}")
    @Operation(summary = "Quick search", description = "Simple text search across all types with a limit.")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<SearchResultDTO> quickSearch(
            @PathVariable String query,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "5") int limit) {

        log.info("Quick search for query: {}, limit: {}", query, limit);
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