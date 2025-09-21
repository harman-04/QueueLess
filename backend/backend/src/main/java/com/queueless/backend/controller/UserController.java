package com.queueless.backend.controller;

import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.dto.UserProfileUpdateRequest;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.queueless.backend.security.annotations.Authenticated; // New import

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PlaceService placeService;

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Authentication context is null or not authenticated.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }
        String userId = authentication.getName();
        log.debug("Current authenticated user ID: {}", userId);
        return userId;
    }

    /**
     * Endpoint to update user profile information.
     */
    @PutMapping("/profile")
    @Authenticated
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        String userId = getCurrentUserId();
        log.info("Starting API call: PUT /api/user/profile for userId: {}", userId);
        try {
            userService.updateUserProfile(userId, request);
            log.info("Successfully updated profile for userId: {}", userId);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            log.error("Failed to update profile for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Endpoint to change the user's password.
     */
    @PutMapping("/password")
    @Authenticated
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        String userId = getCurrentUserId();
        log.info("Starting API call: PUT /api/user/password for userId: {}", userId);
        try {
            userService.changePassword(userId, request);
            log.info("Successfully changed password for userId: {}", userId);
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            log.error("Failed to change password for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Endpoint to delete the user's account.
     */
    @DeleteMapping("/account")
    @Authenticated
    public ResponseEntity<?> deleteAccount() {
        String userId = getCurrentUserId();
        log.info("Starting API call: DELETE /api/user/account for userId: {}", userId);
        try {
            userService.deleteAccount(userId);
            log.info("Successfully deleted account for userId: {}", userId);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete account for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete account.");
        }
    }

    /**
     * Endpoint to get a list of favorite place IDs.
     */
    @GetMapping("/favorites")
    @Authenticated
    public ResponseEntity<List<String>> getFavoritePlaces() {
        String userId = getCurrentUserId();
        log.info("Starting API call: GET /api/user/favorites for userId: {}", userId);

        try {
            List<String> favoritePlaces = userService.getFavoritePlaces(userId);
            log.info("Successfully retrieved {} favorite place IDs for userId: {}", favoritePlaces.size(), userId);
            return ResponseEntity.ok(favoritePlaces);
        } catch (Exception e) {
            log.error("Failed to get favorite place IDs for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint to add a place to favorites.
     */
    @PostMapping("/favorites/{placeId}")
    @Authenticated
    public ResponseEntity<?> addFavoritePlace(@PathVariable String placeId) {
        String userId = getCurrentUserId();
        log.info("Starting API call: POST /api/user/favorites/{} for userId: {}", placeId, userId);

        try {
            userService.addFavoritePlace(userId, placeId);
            log.info("Successfully added place {} to favorites for userId: {}", placeId, userId);
            return ResponseEntity.ok("Place added to favorites");
        } catch (Exception e) {
            log.error("Failed to add favorite place {} for userId: {}. Error: {}", placeId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add favorite place");
        }
    }

    /**
     * Endpoint to remove a place from favorites.
     */
    @DeleteMapping("/favorites/{placeId}")
    @Authenticated
    public ResponseEntity<?> removeFavoritePlace(@PathVariable String placeId) {
        String userId = getCurrentUserId();
        log.info("Starting API call: DELETE /api/user/favorites/{} for userId: {}", placeId, userId);

        try {
            userService.removeFavoritePlace(userId, placeId);
            log.info("Successfully removed place {} from favorites for userId: {}", placeId, userId);
            return ResponseEntity.ok("Place removed from favorites");
        } catch (Exception e) {
            log.error("Failed to remove favorite place {} for userId: {}. Error: {}", placeId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove favorite place");
        }
    }

    /**
     * Endpoint to get a list of favorite places with full details.
     */
    @GetMapping("/favorites/details")
    @Authenticated
    public ResponseEntity<List<PlaceDTO>> getFavoritePlacesWithDetails() {
        String userId = getCurrentUserId();
        log.info("Starting API call: GET /api/user/favorites/details for userId: {}", userId);

        try {
            List<String> favoritePlaceIds = userService.getFavoritePlaces(userId);
            log.debug("Found {} favorite place IDs for userId: {}", favoritePlaceIds.size(), userId);

            List<Place> favoritePlaces = placeService.getPlacesByIds(favoritePlaceIds);
            log.debug("Retrieved details for {} favorite places.", favoritePlaces.size());

            List<PlaceDTO> favoritePlacesDTO = favoritePlaces.stream()
                    .map(PlaceDTO::fromEntity)
                    .collect(Collectors.toList());

            log.info("Successfully retrieved favorite places with details for userId: {}", userId);
            return ResponseEntity.ok(favoritePlacesDTO);
        } catch (Exception e) {
            log.error("Failed to get favorite places with details for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}