package com.queueless.backend.controller;

import com.queueless.backend.dto.FeedbackDTO;
import com.queueless.backend.model.Feedback;
import com.queueless.backend.model.Queue;
import com.queueless.backend.service.FeedbackService;
import com.queueless.backend.service.QueueService;
import com.queueless.backend.security.annotations.UserOnly;
import com.queueless.backend.security.annotations.Authenticated;
import com.queueless.backend.security.annotations.AdminOrProviderOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Endpoints for submitting and retrieving feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final QueueService queueService;

    @PostMapping
    @UserOnly
    @Operation(summary = "Submit feedback", description = "Submits feedback for a completed token. Only the user who owned the token can submit.")
    @ApiResponse(responseCode = "200", description = "Feedback submitted",
            content = @Content(schema = @Schema(implementation = Feedback.class)))
    @ApiResponse(responseCode = "409", description = "Feedback already exists for this token")
    @ApiResponse(responseCode = "404", description = "Queue not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<Feedback> submitFeedback(@Valid @RequestBody FeedbackDTO feedbackDTO, Authentication authentication) {
        String userId = authentication.getName();
        log.info("Received feedback submission request from user {} for token {}", userId, feedbackDTO.getTokenId());

        try {
            log.debug("Checking for existing feedback for token: {}", feedbackDTO.getTokenId());
            boolean alreadySubmitted = feedbackService.hasUserProvidedFeedbackForToken(userId, feedbackDTO.getTokenId());
            if (alreadySubmitted) {
                log.warn("User {} has already submitted feedback for token {}. Request rejected.", userId, feedbackDTO.getTokenId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
            }
            log.debug("No existing feedback found for token: {}", feedbackDTO.getTokenId());

            log.debug("Fetching queue details for queue ID: {}", feedbackDTO.getQueueId());
            Queue queue = queueService.getQueueById(feedbackDTO.getQueueId());
            if (queue == null) {
                log.warn("Queue with ID {} not found. Feedback submission failed.", feedbackDTO.getQueueId());
                return ResponseEntity.notFound().build();
            }
            log.info("Queue details found for ID {}. Proceeding with feedback creation.", feedbackDTO.getQueueId());

            Feedback feedback = new Feedback();
            feedback.setTokenId(feedbackDTO.getTokenId());
            feedback.setQueueId(feedbackDTO.getQueueId());
            feedback.setUserId(userId);
            feedback.setProviderId(queue.getProviderId());
            feedback.setPlaceId(queue.getPlaceId());
            feedback.setServiceId(queue.getServiceId());
            feedback.setRating(feedbackDTO.getRating());
            feedback.setComment(feedbackDTO.getComment());
            feedback.setStaffRating(feedbackDTO.getStaffRating());
            feedback.setServiceRating(feedbackDTO.getServiceRating());
            feedback.setWaitTimeRating(feedbackDTO.getWaitTimeRating());
            feedback.setCreatedAt(LocalDateTime.now());

            log.debug("Saving new feedback entry for token: {}", feedbackDTO.getTokenId());
            Feedback savedFeedback = feedbackService.submitFeedback(feedback);
            log.info("Successfully submitted feedback for token {}. Feedback ID: {}", feedbackDTO.getTokenId(), savedFeedback.getId());
            return ResponseEntity.ok(savedFeedback);
        } catch (Exception e) {
            log.error("Error submitting feedback for user {} and token {}: {}", userId, feedbackDTO.getTokenId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/token/{tokenId}")
    @Authenticated
    @Operation(summary = "Get feedback by token ID", description = "Returns feedback for a specific token. Accessible by the token owner, admin, or provider.")
    @ApiResponse(responseCode = "200", description = "Feedback found",
            content = @Content(schema = @Schema(implementation = Feedback.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden – not authorized to view this feedback")
    @ApiResponse(responseCode = "404", description = "Feedback not found")
    public ResponseEntity<Feedback> getFeedbackByTokenId(@PathVariable String tokenId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Received request to get feedback by token ID: {} from user {}", tokenId, userId);
        Optional<Feedback> feedbackOptional = feedbackService.getFeedbackByTokenId(tokenId);

        if (feedbackOptional.isPresent()) {
            Feedback feedback = feedbackOptional.get();

            // Check if the authenticated user has access
            if (!feedback.getUserId().equals(userId) &&
                    !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PROVIDER"))) {
                log.warn("Unauthorized attempt by user {} to access feedback for token {}.", userId, tokenId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            log.info("Found feedback for token ID: {}", tokenId);
            return ResponseEntity.ok(feedback);
        } else {
            log.info("No feedback found for token ID: {}", tokenId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/place/{placeId}")
    @Operation(summary = "Get feedback by place ID", description = "Returns all feedback for a specific place. Public access.")
    @ApiResponse(responseCode = "200", description = "List of feedback")
    public ResponseEntity<List<Feedback>> getFeedbackByPlaceId(@PathVariable String placeId) {
        log.info("Received request to get feedback for place ID: {}", placeId);
        List<Feedback> feedbacks = feedbackService.getFeedbackByPlaceId(placeId);
        log.info("Found {} feedbacks for place ID: {}", feedbacks.size(), placeId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/provider/{providerId}")
    @AdminOrProviderOnly
    @Operation(summary = "Get feedback by provider ID", description = "Returns all feedback for a specific provider. Providers can only access their own; admins can access any.")
    @ApiResponse(responseCode = "200", description = "List of feedback")
    @ApiResponse(responseCode = "403", description = "Forbidden – provider accessing another provider's data")
    public ResponseEntity<List<Feedback>> getFeedbackByProviderId(@PathVariable String providerId, Authentication authentication) {
        String requesterId = authentication.getName();

        // Ensure providers can only access their own feedback
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER")) &&
                !requesterId.equals(providerId)) {
            log.warn("Unauthorized access attempt. Provider {} tried to access feedback for provider {}.", requesterId, providerId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Received request to get feedback for provider ID: {} from user {}", providerId, requesterId);
        List<Feedback> feedbacks = feedbackService.getFeedbackByProviderId(providerId);
        log.info("Found {} feedbacks for provider ID: {}", feedbacks.size(), providerId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/place/{placeId}/average-rating")
    @Operation(summary = "Get average rating for place", description = "Returns the average overall rating for a place. Public access.")
    @ApiResponse(responseCode = "200", description = "Average rating (0.0 if none)")
    public ResponseEntity<Double> getAverageRatingForPlace(@PathVariable String placeId) {
        log.info("Received request to get average rating for place ID: {}", placeId);
        try {
            Double averageRating = feedbackService.getAverageRatingForPlace(placeId);
            log.info("Calculated average rating for place {}: {}", placeId, averageRating);
            return ResponseEntity.ok(averageRating != null ? averageRating : 0.0);
        } catch (Exception e) {
            log.error("Error getting average rating for place {}: {}", placeId, e.getMessage(), e);
            return ResponseEntity.ok(0.0); // Return 0 on error
        }
    }

    @GetMapping("/provider/{providerId}/average-rating")
    @Operation(summary = "Get average rating for provider", description = "Returns the average overall rating for a provider. Public access.")
    @ApiResponse(responseCode = "200", description = "Average rating (0.0 if none)")
    public ResponseEntity<Double> getAverageRatingForProvider(@PathVariable String providerId) {
        log.info("Received request to get average rating for provider ID: {}", providerId);
        try {
            Double averageRating = feedbackService.getAverageRatingForProvider(providerId);
            log.info("Calculated average rating for provider {}: {}", providerId, averageRating);
            return ResponseEntity.ok(averageRating != null ? averageRating : 0.0);
        } catch (Exception e) {
            log.error("Error getting average rating for provider {}: {}", providerId, e.getMessage(), e);
            return ResponseEntity.ok(0.0); // Return 0 on error
        }
    }

    @GetMapping("/user/{userId}/token/{tokenId}")
    @UserOnly
    @Operation(summary = "Check if user provided feedback for a token", description = "Checks whether a specific user has already submitted feedback for a given token.")
    @ApiResponse(responseCode = "200", description = "Boolean indicating if feedback exists")
    @ApiResponse(responseCode = "403", description = "Forbidden – user mismatch")
    public ResponseEntity<Boolean> hasUserProvidedFeedback(@PathVariable String userId, @PathVariable String tokenId, Authentication authentication) {
        log.info("Received request to check if user {} provided feedback for token {}", userId, tokenId);

        if (!authentication.getName().equals(userId)) {
            log.warn("Unauthorized attempt by user {} to check feedback status for user {}.", authentication.getName(), userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.debug("Authenticated user {} is authorized to check feedback for token {}.", userId, tokenId);

        boolean hasProvidedFeedback = feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId);
        log.info("User {} has provided feedback for token {}: {}", userId, tokenId, hasProvidedFeedback);
        return ResponseEntity.ok(hasProvidedFeedback);
    }

    @GetMapping("/place/{placeId}/detailed-ratings")
    @Operation(summary = "Get detailed ratings for place", description = "Returns overall, staff, service, and wait time average ratings for a place.")
    @ApiResponse(responseCode = "200", description = "Map of rating categories to averages")
    public ResponseEntity<Map<String, Double>> getDetailedRatingsForPlace(@PathVariable String placeId) {
        log.info("Received request for detailed ratings for place ID: {}", placeId);
        try {
            Map<String, Double> ratings = feedbackService.getAllAverageRatingsForPlace(placeId);
            log.info("Successfully retrieved detailed ratings for place {}.", placeId);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            log.error("Error getting detailed ratings for place {}: {}", placeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent feedback", description = "Returns a limited number of recent feedback entries for public display.")
    public ResponseEntity<List<FeedbackDTO>> getRecentFeedback(@RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching {} recent feedback entries", limit);
        List<Feedback> recent = feedbackService.getRecentFeedback(limit);
        List<FeedbackDTO> dtos = recent.stream()
                .map(this::convertToDTO)  // you may need a proper mapper
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private FeedbackDTO convertToDTO(Feedback feedback) {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setTokenId(feedback.getTokenId());
        dto.setQueueId(feedback.getQueueId());
        dto.setRating(feedback.getRating());
        dto.setComment(feedback.getComment());
        // include user name if available (you may need to fetch user)
        return dto;
    }
}