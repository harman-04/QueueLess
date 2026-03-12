package com.queueless.backend.controller;

import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.PlaceDTO;
import com.queueless.backend.dto.UserProfileUpdateRequest;
import com.queueless.backend.dto.UserTokenHistoryDTO;
import com.queueless.backend.model.Place;
import com.queueless.backend.service.PlaceService;
import com.queueless.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.queueless.backend.security.annotations.Authenticated;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Endpoints for user profile management and favorites")
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

    @PutMapping("/profile")
    @Authenticated
    @Operation(summary = "Update user profile", description = "Updates the authenticated user's profile information (name, phone, profile image).")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or user not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @PutMapping("/password")
    @Authenticated
    @Operation(summary = "Change password", description = "Changes the authenticated user's password.")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or incorrect current password")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @DeleteMapping("/account")
    @Authenticated
    @Operation(summary = "Delete account", description = "Permanently deletes the authenticated user's account.")
    @ApiResponse(responseCode = "200", description = "Account deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "500", description = "Internal server error")
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

    @GetMapping("/favorites")
    @Authenticated
    @Operation(summary = "Get favorite place IDs", description = "Returns a list of place IDs that the user has marked as favorite.")
    @ApiResponse(responseCode = "200", description = "List of favorite place IDs")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @PostMapping("/favorites/{placeId}")
    @Authenticated
    @Operation(summary = "Add favorite place", description = "Adds a place to the user's favorites.")
    @ApiResponse(responseCode = "200", description = "Place added to favorites")
    @ApiResponse(responseCode = "400", description = "Invalid place ID")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @DeleteMapping("/favorites/{placeId}")
    @Authenticated
    @Operation(summary = "Remove favorite place", description = "Removes a place from the user's favorites.")
    @ApiResponse(responseCode = "200", description = "Place removed from favorites")
    @ApiResponse(responseCode = "400", description = "Invalid place ID")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @GetMapping("/favorites/details")
    @Authenticated
    @Operation(summary = "Get favorite places with details", description = "Returns full details of all places the user has favorited.")
    @ApiResponse(responseCode = "200", description = "List of favorite places with details")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
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

    @GetMapping("/analytics/token-history")
    @Authenticated
    @Operation(summary = "Get user token history", description = "Returns daily token counts for the last N days (default 30).")
    public ResponseEntity<Map<String, Object>> getUserTokenHistory(
            @RequestParam(defaultValue = "30") int days) {
        String userId = getCurrentUserId();
        Map<String, Object> data = userService.getUserTokenHistory(userId, days);
        return ResponseEntity.ok(data);
    }


    @PostMapping("/fcm-token")
    @Authenticated
    @Operation(summary = "Register FCM token", description = "Adds a device token for push notifications.")
    public ResponseEntity<?> registerFcmToken(@RequestParam String token) {
        String userId = getCurrentUserId();
        userService.addFcmToken(userId, token);
        return ResponseEntity.ok("Token registered");
    }

    @DeleteMapping("/fcm-token")
    @Authenticated
    @Operation(summary = "Unregister FCM token", description = "Removes a device token.")
    public ResponseEntity<?> unregisterFcmToken(@RequestParam String token) {
        String userId = getCurrentUserId();
        userService.removeFcmToken(userId, token);
        return ResponseEntity.ok("Token removed");
    }

    @GetMapping("/tokens")
    @Authenticated
    @Operation(summary = "Get user token history", description = "Returns list of completed/cancelled tokens with details.")
    @ApiResponse(responseCode = "200", description = "Token history")
    public ResponseEntity<List<UserTokenHistoryDTO>> getUserTokenHistory(
            @RequestParam(defaultValue = "30") int days,
            @PageableDefault(size = 20, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = getCurrentUserId();
        List<UserTokenHistoryDTO> history = userService.getUserTokenHistoryOptimized(userId, days, pageable);
        return ResponseEntity.ok(history);
    }
}