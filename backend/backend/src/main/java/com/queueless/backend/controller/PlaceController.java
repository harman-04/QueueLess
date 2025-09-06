package com.queueless.backend.controller;

import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.PlaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;



    private String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // Now returns userId
        }
        return null;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlaceDTO> createPlace(@Valid @RequestBody PlaceDTO placeDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = getUserIdFromAuthentication(authentication);

        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for createPlace");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Request received to create place: {} by admin: {}", placeDTO, adminId);

        // Ensure the adminId in DTO matches the authenticated admin
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<List<PlaceDTO>> getPlacesByType(@PathVariable String type) {
        log.debug("Fetching places of type: {}", type);
        List<Place> places = placeService.getPlacesByType(type);
        log.info("Fetched {} places of type {}", places.size(), type);
        return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PlaceDTO>> getNearbyPlaces(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "5") double radius) {
        log.debug("Searching nearby places [lon={}, lat={}, radius={}km]", longitude, latitude, radius);
        List<Place> places = placeService.getNearbyPlaces(longitude, latitude, radius);
        log.info("Found {} nearby places", places.size());
        return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlaceDTO> updatePlace(@PathVariable String id, @Valid @RequestBody PlaceDTO placeDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for updatePlace");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.info("Updating place with ID: {} | Data: {} by admin: {}", id, placeDTO, adminId);

        try {
            // Verify the place belongs to the authenticated admin
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlace(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            log.error("Authentication is null for deletePlace");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String adminId = authentication.getName();
        log.warn("Request received to delete place with ID: {} by admin: {}", id, adminId);

        try {
            // Verify the place belongs to the authenticated admin
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
    public ResponseEntity<List<PlaceDTO>> getAllPlaces() {
        log.debug("Fetching all places");
        List<Place> places = placeService.getAllPlaces();
        log.info("Fetched {} places", places.size());
        return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
    }
}
