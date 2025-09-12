package com.queueless.backend.service;

import com.queueless.backend.model.Feedback;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.FeedbackRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final QueueRepository queueRepository;

    public Feedback submitFeedback(Feedback feedback) {
        log.info("Attempting to submit feedback for token: {}", feedback.getTokenId());
        try {
            log.debug("Fetching queue details for queue ID: {}", feedback.getQueueId());
            // Get queue information to populate provider/place details
            Queue queue = queueRepository.findById(feedback.getQueueId())
                    .orElseThrow(() -> new RuntimeException("Queue not found"));
            log.debug("Queue details found for ID: {}. Populating feedback.", feedback.getQueueId());

            // Set additional details from the queue
            feedback.setProviderId(queue.getProviderId());
            feedback.setPlaceId(queue.getPlaceId());
            feedback.setServiceId(queue.getServiceId());

            // Save the feedback
            log.debug("Saving new feedback entry to the database.");
            Feedback savedFeedback = feedbackRepository.save(feedback);
            log.info("Feedback successfully saved with ID: {}", savedFeedback.getId());

            // Update place ratings
            log.debug("Triggering update of place ratings for place ID: {}", feedback.getPlaceId());
            updatePlaceRatings(feedback.getPlaceId());

            // Update provider ratings if provider exists
            if (feedback.getProviderId() != null) {
                log.debug("Triggering update of provider ratings for provider ID: {}", feedback.getProviderId());
                updateProviderRatings(feedback.getProviderId());
            }

            log.info("Feedback submission process completed for token: {}", feedback.getTokenId());
            return savedFeedback;
        } catch (Exception e) {
            log.error("Failed to submit feedback for token {}: {}", feedback.getTokenId(), e.getMessage(), e);
            throw new RuntimeException("Failed to submit feedback: " + e.getMessage());
        }
    }

    public Optional<Feedback> getFeedbackByTokenId(String tokenId) {
        log.info("Fetching feedback by token ID: {}", tokenId);
        Optional<Feedback> feedback = feedbackRepository.findByTokenId(tokenId);
        feedback.ifPresentOrElse(
                f -> log.info("Found feedback for token: {}", tokenId),
                () -> log.info("No feedback found for token: {}", tokenId)
        );
        return feedback;
    }

    public List<Feedback> getFeedbackByPlaceId(String placeId) {
        log.info("Fetching all feedbacks for place ID: {}", placeId);
        List<Feedback> feedbacks = feedbackRepository.findByPlaceId(placeId);
        log.info("Found {} feedbacks for place ID: {}", feedbacks.size(), placeId);
        return feedbacks;
    }

    public List<Feedback> getFeedbackByProviderId(String providerId) {
        log.info("Fetching all feedbacks for provider ID: {}", providerId);
        List<Feedback> feedbacks = feedbackRepository.findByProviderId(providerId);
        log.info("Found {} feedbacks for provider ID: {}", feedbacks.size(), providerId);
        return feedbacks;
    }

    public Double getAverageRatingForPlace(String placeId) {
        log.info("Calculating average rating for place ID: {}", placeId);
        List<Feedback> feedbacks = feedbackRepository.findByPlaceId(placeId);
        if (feedbacks.isEmpty()) {
            log.info("No feedbacks found for place ID {}. Returning 0.0.", placeId);
            return 0.0;
        }
        double averageRating = feedbacks.stream()
                .filter(feedback -> feedback.getRating() != null)
                .mapToDouble(Feedback::getRating)
                .average()
                .orElse(0.0);
        log.info("Calculated average rating for place {}: {}", placeId, averageRating);
        return averageRating;
    }

    public Double getAverageRatingForProvider(String providerId) {
        log.info("Calculating average rating for provider ID: {}", providerId);
        List<Feedback> feedbacks = feedbackRepository.findByProviderId(providerId);
        if (feedbacks.isEmpty()) {
            log.info("No feedbacks found for provider ID {}. Returning 0.0.", providerId);
            return 0.0;
        }
        double averageRating = feedbacks.stream()
                .filter(feedback -> feedback.getRating() != null)
                .mapToDouble(Feedback::getRating)
                .average()
                .orElse(0.0);
        log.info("Calculated average rating for provider {}: {}", providerId, averageRating);
        return averageRating;
    }

    private void updatePlaceRatings(String placeId) {
        log.debug("Starting update of place ratings for place ID: {}", placeId);
        List<Feedback> feedbacks = feedbackRepository.findByPlaceId(placeId);
        if (feedbacks.isEmpty()) {
            log.debug("No feedbacks to update ratings for place ID {}. Skipping.", placeId);
            return;
        }
        log.debug("Found {} feedbacks for place ID {}. Calculating new ratings.", feedbacks.size(), placeId);

        double averageRating = feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .mapToDouble(Feedback::getRating)
                .average()
                .orElse(0.0);

        long totalRatings = feedbacks.size();

        double staffRating = feedbacks.stream()
                .filter(f -> f.getStaffRating() != null)
                .mapToDouble(Feedback::getStaffRating)
                .average()
                .orElse(0.0);

        double serviceRating = feedbacks.stream()
                .filter(f -> f.getServiceRating() != null)
                .mapToDouble(Feedback::getServiceRating)
                .average()
                .orElse(0.0);

        double waitTimeRating = feedbacks.stream()
                .filter(f -> f.getWaitTimeRating() != null)
                .mapToDouble(Feedback::getWaitTimeRating)
                .average()
                .orElse(0.0);

        Optional<Place> placeOpt = placeRepository.findById(placeId);
        if (placeOpt.isPresent()) {
            Place place = placeOpt.get();
            place.setRating(averageRating);
            place.setTotalRatings((int) totalRatings);
            placeRepository.save(place);
            log.info("Updated ratings for place {}. New overall rating: {}", placeId, averageRating);
        } else {
            log.warn("Place with ID {} not found while trying to update ratings. Data might be inconsistent.", placeId);
        }
    }

    private void updateProviderRatings(String providerId) {
        log.debug("Starting update of provider ratings for provider ID: {}", providerId);
        List<Feedback> feedbacks = feedbackRepository.findByProviderId(providerId);
        if (feedbacks.isEmpty()) {
            log.debug("No feedbacks to update ratings for provider ID {}. Skipping.", providerId);
            return;
        }
        log.debug("Found {} feedbacks for provider ID {}. Calculating new average rating.", feedbacks.size(), providerId);

        double averageRating = feedbacks.stream()
                .filter(f -> f.getRating() != null)
                .mapToDouble(Feedback::getRating)
                .average()
                .orElse(0.0);

        Optional<User> userOpt = userRepository.findById(providerId);
        if (userOpt.isPresent()) {
            User provider = userOpt.get();
            log.info("Provider {} would have average rating: {}. (Note: This is a log-only update, user entity does not have a rating field).", providerId, averageRating);
            // The original code doesn't save the rating, so this log entry is an observation.
        } else {
            log.warn("Provider with ID {} not found while trying to update ratings. Data might be inconsistent.", providerId);
        }
    }

    public boolean hasUserProvidedFeedbackForToken(String userId, String tokenId) {
        log.info("Checking if user {} has provided feedback for token {}", userId, tokenId);
        boolean hasFeedback = feedbackRepository.findByTokenId(tokenId)
                .map(feedback -> feedback.getUserId().equals(userId))
                .orElse(false);
        log.info("User {} has provided feedback for token {}: {}", userId, tokenId, hasFeedback);
        return hasFeedback;
    }

    public Map<String, Double> getAllAverageRatingsForPlace(String placeId) {
        log.info("Calculating detailed average ratings for place ID: {}", placeId);
        List<Feedback> feedbacks = feedbackRepository.findByPlaceId(placeId);
        if (feedbacks.isEmpty()) {
            log.info("No feedbacks found for place ID {}. Returning all ratings as 0.0.", placeId);
            return Map.of(
                    "overall", 0.0,
                    "staff", 0.0,
                    "service", 0.0,
                    "waitTime", 0.0
            );
        }
        log.debug("Found {} feedbacks. Calculating detailed ratings.", feedbacks.size());

        // Calculate averages for each category
        double overall = feedbacks.stream().filter(f -> f.getRating() != null).mapToDouble(Feedback::getRating).average().orElse(0.0);
        double staff = feedbacks.stream().filter(f -> f.getStaffRating() != null).mapToDouble(Feedback::getStaffRating).average().orElse(0.0);
        double service = feedbacks.stream().filter(f -> f.getServiceRating() != null).mapToDouble(Feedback::getServiceRating).average().orElse(0.0);
        double waitTime = feedbacks.stream().filter(f -> f.getWaitTimeRating() != null).mapToDouble(Feedback::getWaitTimeRating).average().orElse(0.0);

        log.info("Detailed ratings for place {} calculated: overall={}, staff={}, service={}, waitTime={}",
                placeId, overall, staff, service, waitTime);
        return Map.of(
                "overall", overall,
                "staff", staff,
                "service", service,
                "waitTime", waitTime
        );
    }
}