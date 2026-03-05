package com.queueless.backend.controller;

import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.security.annotations.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "Places", description = "Endpoints for managing places")
public class PlaceController {

    private final PlaceService placeService;

    private String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

    @PostMapping
    @AdminOnly
    @Operation(summary = "Create a new place", description = "Creates a new place. Only accessible by admin.")
    @ApiResponse(responseCode = "200", description = "Place created successfully",
            content = @Content(schema = @Schema(implementation = PlaceDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – admin ID mismatch")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<PlaceDTO> createPlace(@Valid @RequestBody PlaceDTO placeDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = getUserIdFromAuthentication(authentication);

        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for createPlace");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Request received to create place: {} by admin: {}", placeDTO, adminId);

        if (!placeDTO.getAdminId().equals(adminId)) {
            log.warn("Unauthorized attempt to create place for adminId={} by {}", placeDTO.getAdminId(), adminId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Place place = placeService.createPlace(placeDTO);
            log.info("Place created successfully with ID: {}", place.getId());
            return ResponseEntity.ok(PlaceDTO.fromEntity(place));
        } catch (Exception e) {
            log.error("Error creating place: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get place by ID", description = "Returns a single place by its ID. Public access.")
    @ApiResponse(responseCode = "200", description = "Place found",
            content = @Content(schema = @Schema(implementation = PlaceDTO.class)))
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<PlaceDTO> getPlace(@PathVariable String id) {
        log.debug("Fetching place with ID: {}", id);
        try {
            Place place = placeService.getPlaceById(id);
            log.info("Fetched place: {}", place);
            return ResponseEntity.ok(PlaceDTO.fromEntity(place));
        } catch (Exception e) {
            log.error("Error fetching place: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/admin/my-places")
    @AdminOnly
    @Operation(summary = "Get my places", description = "Returns all places managed by the authenticated admin.")
    @ApiResponse(responseCode = "200", description = "List of places")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<PlaceDTO>> getMyPlaces() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = getUserIdFromAuthentication(authentication);

        if (adminId == null) {
            log.error("Admin ID not found in authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Place> places = placeService.getPlacesByAdminId(adminId);
            return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error fetching places for admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admin/{adminId}")
    @AdminOnly
    @Operation(summary = "Get places by admin ID", description = "Returns all places for a specific admin. The authenticated admin must match the requested adminId.")
    @ApiResponse(responseCode = "200", description = "List of places")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – admin ID mismatch")
    public ResponseEntity<List<PlaceDTO>> getPlacesByAdmin(@PathVariable String adminId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.error("Authentication is null for getPlacesByAdmin");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentAdminId = getUserIdFromAuthentication(authentication);
        if (currentAdminId == null || !adminId.equals(currentAdminId)) {
            log.warn("Unauthorized attempt to access places of adminId={} by {}", adminId, currentAdminId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.debug("Fetching places for admin ID: {}", adminId);
        try {
            List<Place> places = placeService.getPlacesByAdminId(adminId);
            log.info("Fetched {} places for admin {}", places.size(), adminId);
            return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error fetching places by admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get places by type", description = "Returns all places of a given type. Public access.")
    @ApiResponse(responseCode = "200", description = "List of places")
    public ResponseEntity<List<PlaceDTO>> getPlacesByType(@PathVariable String type) {
        log.debug("Fetching places of type: {}", type);
        List<Place> places = placeService.getPlacesByType(type);
        log.info("Fetched {} places of type {}", places.size(), type);
        return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby places", description = "Returns places within a given radius (km) from the specified coordinates. Public access.")
    @ApiResponse(responseCode = "200", description = "List of nearby places")
    public ResponseEntity<List<PlaceDTO>> getNearbyPlaces(
            @Parameter(description = "Longitude") @RequestParam double longitude,
            @Parameter(description = "Latitude") @RequestParam double latitude,
            @Parameter(description = "Radius in kilometers") @RequestParam(defaultValue = "5") double radius) {
        log.debug("Searching nearby places [lon={}, lat={}, radius={}km]", longitude, latitude, radius);
        List<Place> places = placeService.getNearbyPlaces(longitude, latitude, radius);
        log.info("Found {} nearby places", places.size());
        return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Update a place", description = "Updates an existing place. Only the owning admin can update.")
    @ApiResponse(responseCode = "200", description = "Place updated",
            content = @Content(schema = @Schema(implementation = PlaceDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the owner")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<PlaceDTO> updatePlace(@PathVariable String id, @Valid @RequestBody PlaceDTO placeDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for updatePlace");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.info("Updating place with ID: {} | Data: {} by admin: {}", id, placeDTO, adminId);

        try {
            if (!placeService.isPlaceOwnedByAdmin(id, adminId)) {
                log.warn("Unauthorized attempt to update place={} by {}", id, adminId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Place place = placeService.updatePlace(id, placeDTO);
            log.info("Place updated successfully with ID: {}", id);
            return ResponseEntity.ok(PlaceDTO.fromEntity(place));
        } catch (Exception e) {
            log.error("Error updating place: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Delete a place", description = "Deletes an existing place. Only the owning admin can delete.")
    @ApiResponse(responseCode = "200", description = "Place deleted")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden – not the owner")
    @ApiResponse(responseCode = "404", description = "Place not found")
    public ResponseEntity<Void> deletePlace(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for deletePlace");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.warn("Request received to delete place with ID: {} by admin: {}", id, adminId);

        try {
            if (!placeService.isPlaceOwnedByAdmin(id, adminId)) {
                log.warn("Unauthorized attempt to delete place={} by {}", id, adminId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            placeService.deletePlace(id);
            log.info("Place deleted successfully with ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting place: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all places (non-paginated)", description = "Returns all places. Public access. Consider using /paginated for large datasets.")
    @ApiResponse(responseCode = "200", description = "List of all places")
    public ResponseEntity<List<PlaceDTO>> getAllPlaces() {
        log.debug("Fetching all places");
        List<Place> places = placeService.getAllPlaces();
        log.info("Fetched {} places", places.size());
        return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get paginated places", description = "Returns a paginated list of places. Public access.")
    @ApiResponse(responseCode = "200", description = "Paginated list of places")
    public ResponseEntity<Page<PlaceDTO>> getPlacesPaginated(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.debug("Fetching paginated places: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Place> placePage = placeService.getAllPlacesPaginated(pageable);
        Page<PlaceDTO> dtoPage = placePage.map(PlaceDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top-rated places", description = "Returns a limited number of places with the highest average rating.")
    public ResponseEntity<List<PlaceDTO>> getTopRatedPlaces(@RequestParam(defaultValue = "3") int limit) {
        log.info("Fetching top {} rated places", limit);
        List<Place> places = placeService.getTopRatedPlaces(limit);
        List<PlaceDTO> dtos = places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}