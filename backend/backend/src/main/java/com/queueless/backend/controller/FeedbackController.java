package com.queueless.backend.controller;

import com.queueless.backend.dto.FeedbackDTO;
import com.queueless.backend.model.Feedback;
import com.queueless.backend.model.Queue;
import com.queueless.backend.service.FeedbackService;
import com.queueless.backend.service.QueueService;
import com.queueless.backend.security.annotations.UserOnly;
import com.queueless.backend.security.annotations.Authenticated;
import com.queueless.backend.security.annotations.AdminOrProviderOnly;
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

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final QueueService queueService;

    @PostMapping
    @UserOnly
    public ResponseEntity<Feedback> submitFeedback(@RequestBody FeedbackDTO feedbackDTO, Authentication authentication) {
        String userId = authentication.getName();
        log.info("Received feedback submission request from user {} for token {}", userId, feedbackDTO.getTokenId());

        try {
            log.debug("Checking for existing feedback for token: {}", feedbackDTO.getTokenId());
            Optional<Feedback> existingFeedback = feedbackService.getFeedbackByTokenId(feedbackDTO.getTokenId());
            if (existingFeedback.isPresent()) {
                log.warn("Feedback for token {} already exists. Request rejected.", feedbackDTO.getTokenId());
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
    public ResponseEntity<List<Feedback>> getFeedbackByPlaceId(@PathVariable String placeId) {
        log.info("Received request to get feedback for place ID: {}", placeId);
        List<Feedback> feedbacks = feedbackService.getFeedbackByPlaceId(placeId);
        log.info("Found {} feedbacks for place ID: {}", feedbacks.size(), placeId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/provider/{providerId}")
    @AdminOrProviderOnly
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
    public ResponseEntity<Boolean> hasUserProvidedFeedback(@PathVariable String userId, @PathVariable String tokenId, Authentication authentication) {
        log.info("Received request to check if user {} provided feedback for token {}", userId, tokenId);

        // This check is a good practice for defense-in-depth, ensuring a user can't check
        // feedback status for another user. The @UserOnly annotation handles the role check.
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
}