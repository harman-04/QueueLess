package com.queueless.backend.controller;

import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.dto.ServiceDTO;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.UserRepository;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.ProviderAnalyticsService;
import com.queueless.backend.service.ServiceService;
import com.queueless.backend.security.annotations.ProviderOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Endpoints for provider-specific data")
public class ProviderController {

    private final UserRepository userRepository;
    private final PlaceService placeService;
    private final ServiceService serviceService;
    private final ProviderAnalyticsService providerAnalyticsService;

    @GetMapping("/my-places")
    @ProviderOnly
    @Operation(summary = "Get my managed places", description = "Returns the places that the authenticated provider manages. If no specific managed places are assigned, returns all places under the admin.")
    @ApiResponse(responseCode = "200", description = "List of places",
            content = @Content(schema = @Schema(implementation = PlaceDTO.class)))
    @ApiResponse(responseCode = "404", description = "Provider not found")
    public ResponseEntity<List<PlaceDTO>> getMyManagedPlaces(Authentication authentication) {
        String providerId = authentication.getName();
        log.info("Fetching managed places for provider: {}", providerId);
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.error("Provider not found with ID: {}", providerId);
                    return new RuntimeException("Provider not found");
                });

        List<PlaceDTO> places;
        if (provider.getManagedPlaceIds() != null && !provider.getManagedPlaceIds().isEmpty()) {
            places = placeService.getPlacesByIds(provider.getManagedPlaceIds()).stream()
                    .map(PlaceDTO::fromEntity)
                    .collect(Collectors.toList());
            log.debug("Found {} places from managedPlaceIds", places.size());
        } else {
            places = placeService.getPlacesByAdminId(provider.getAdminId()).stream()
                    .map(PlaceDTO::fromEntity)
                    .collect(Collectors.toList());
            log.debug("Found {} places from admin ID", places.size());
        }

        log.info("Returning {} places for provider {}", places.size(), providerId);
        return ResponseEntity.ok(places);
    }

    @GetMapping("/my-services")
    @ProviderOnly
    @Operation(summary = "Get my managed services", description = "Returns all services under the places managed by the authenticated provider.")
    @ApiResponse(responseCode = "200", description = "List of services",
            content = @Content(schema = @Schema(implementation = ServiceDTO.class)))
    @ApiResponse(responseCode = "404", description = "Provider not found")
    public ResponseEntity<List<ServiceDTO>> getMyManagedServices(Authentication authentication) {
        String providerId = authentication.getName();
        log.info("Fetching managed services for provider: {}", providerId);
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.error("Provider not found with ID: {}", providerId);
                    return new RuntimeException("Provider not found");
                });

        List<String> placeIds;
        if (provider.getManagedPlaceIds() != null && !provider.getManagedPlaceIds().isEmpty()) {
            placeIds = provider.getManagedPlaceIds();
            log.debug("Using managedPlaceIds: {}", placeIds);
        } else {
            placeIds = placeService.getPlacesByAdminId(provider.getAdminId())
                    .stream().map(place -> place.getId()).collect(Collectors.toList());
            log.debug("Using admin's places: {}", placeIds);
        }

        List<ServiceDTO> services = placeIds.stream()
                .flatMap(placeId -> serviceService.getServicesByPlaceId(placeId).stream())
                .map(ServiceDTO::fromEntity)
                .collect(Collectors.toList());

        log.info("Returning {} services for provider {}", services.size(), providerId);
        return ResponseEntity.ok(services);
    }

    // In ProviderController.java

    @GetMapping("/analytics/tokens-over-time")
    @ProviderOnly
    @Operation(summary = "Get token volume over time for provider", description = "Returns daily token counts for the last N days (default 30).")
    public ResponseEntity<Map<String, Object>> getTokensOverTime(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        String providerId = authentication.getName();
        Map<String, Object> data = providerAnalyticsService.getTokensOverTime(providerId, days);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/analytics/busiest-hours")
    @ProviderOnly
    @Operation(summary = "Get busiest hours for provider", description = "Returns average queue waiting count by hour (0-23) over the last 30 days.")
    public ResponseEntity<Map<Integer, Double>> getBusiestHours(Authentication authentication) {
        String providerId = authentication.getName();
        Map<Integer, Double> data = providerAnalyticsService.getBusiestHours(providerId);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/analytics/average-wait-time")
    @ProviderOnly
    @Operation(summary = "Get average wait time trend for provider", description = "Returns daily average wait time for the last N days (default 30).")
    public ResponseEntity<Map<String, Object>> getAverageWaitTimeTrend(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        String providerId = authentication.getName();
        Map<String, Object> data = providerAnalyticsService.getAverageWaitTimeTrend(providerId, days);
        return ResponseEntity.ok(data);
    }
}