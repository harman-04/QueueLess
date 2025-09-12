package com.queueless.backend.controller;

import com.queueless.backend.dto.FeedbackDTO;
import com.queueless.backend.model.Feedback;
import com.queueless.backend.model.Queue;
import com.queueless.backend.service.FeedbackService;
import com.queueless.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Feedback> submitFeedback(@RequestBody FeedbackDTO feedbackDTO, Authentication authentication) {
        String userId = authentication.getName();
        log.info("Received feedback submission request from user {} for token {}", userId, feedbackDTO.getTokenId());

        try {
            // Check if feedback already exists for this token
            log.debug("Checking for existing feedback for token: {}", feedbackDTO.getTokenId());
            Optional<Feedback> existingFeedback = feedbackService.getFeedbackByTokenId(feedbackDTO.getTokenId());
            if (existingFeedback.isPresent()) {
                log.warn("Feedback for token {} already exists. Request rejected.", feedbackDTO.getTokenId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
            }
            log.debug("No existing feedback found for token: {}", feedbackDTO.getTokenId());

            // Get queue information
            log.debug("Fetching queue details for queue ID: {}", feedbackDTO.getQueueId());
            Queue queue = queueService.getQueueById(feedbackDTO.getQueueId());
            if (queue == null) {
                log.warn("Queue with ID {} not found. Feedback submission failed.", feedbackDTO.getQueueId());
                return ResponseEntity.notFound().build();
            }
            log.info("Queue details found for ID {}. Proceeding with feedback creation.", feedbackDTO.getQueueId());

            // Create new feedback
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
    public ResponseEntity<Feedback> getFeedbackByTokenId(@PathVariable String tokenId) {
        log.info("Received request to get feedback by token ID: {}", tokenId);
        Optional<Feedback> feedback = feedbackService.getFeedbackByTokenId(tokenId);
        if (feedback.isPresent()) {
            log.info("Found feedback for token ID: {}", tokenId);
            return ResponseEntity.ok(feedback.get());
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
    public ResponseEntity<List<Feedback>> getFeedbackByProviderId(@PathVariable String providerId) {
        log.info("Received request to get feedback for provider ID: {}", providerId);
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
    public ResponseEntity<Boolean> hasUserProvidedFeedback(@PathVariable String userId, @PathVariable String tokenId, Authentication authentication) {
        log.info("Received request to check if user {} provided feedback for token {}", userId, tokenId);
        // Verify the authenticated user is checking their own feedback
        if (!authentication.getName().equals(userId)) {
            log.warn("Unauthorized attempt by user {} to check feedback status for user {}.", authentication.getName(), userId);
            return ResponseEntity.status(403).build();
        }
        log.debug("Authenticated user {} is authorized to check feedback for token {}.", userId, tokenId);

        boolean hasProvidedFeedback = feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId);
        log.info("User {} has provided feedback for token {}: {}", userId, tokenId, hasProvidedFeedback);
        return ResponseEntity.ok(hasProvidedFeedback);
    }

    // In FeedbackController.java - Create a new endpoint
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