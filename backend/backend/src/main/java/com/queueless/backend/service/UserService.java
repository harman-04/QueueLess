package com.queueless.backend.service;

import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.UserProfileUpdateRequest;
import com.queueless.backend.dto.UserTokenHistoryDTO;
import com.queueless.backend.enums.TokenStatus;
import com.queueless.backend.exception.ResourceNotFoundException;
import com.queueless.backend.model.*;
import com.queueless.backend.model.Queue;
import com.queueless.backend.repository.FeedbackRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QueueRepository queueRepository;
    private final FeedbackRepository feedbackRepository;
    private final PlaceService placeService;
    private final MongoTemplate mongoTemplate;

    public void updateUserProfile(String userId, UserProfileUpdateRequest request) {
        log.info("Attempting to update user profile for user ID: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (request.getName() != null && !request.getName().isBlank()) {
                user.setName(request.getName());
                log.debug("Updating name for user {}: {}", userId, request.getName());
            }
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
                user.setPhoneNumber(request.getPhoneNumber());
                log.debug("Updating phone number for user {}: {}", userId, request.getPhoneNumber());
            }
            if (request.getProfileImageUrl() != null) {
                user.setProfileImageUrl(request.getProfileImageUrl());
                log.debug("Updating profile image URL for user {}: {}", userId, request.getProfileImageUrl());
            }

            userRepository.save(user);
            log.info("User profile successfully updated for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user profile for user ID: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }

    public void changePassword(String userId, PasswordChangeRequest request) {
        log.info("Attempting to change password for user ID: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Password change failed for user {}: Incorrect current password.", userId);
                throw new RuntimeException("Incorrect current password");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            log.info("Password successfully changed for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to change password for user ID: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }

    public void deleteAccount(String userId) {
        log.info("Attempting to delete account for user ID: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            userRepository.delete(user);
            log.info("Account for user ID: {} has been successfully deleted.", userId);
        } catch (Exception e) {
            log.error("Failed to delete account for user ID: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }

    public List<String> getFavoritePlaces(String userId) {
        log.info("Fetching favorite places for user ID: {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> favorites = (user.getPreferences() != null && user.getPreferences().getFavoritePlaceIds() != null)
                    ? user.getPreferences().getFavoritePlaceIds()
                    : new ArrayList<>();
            log.info("Successfully fetched {} favorite places for user ID: {}", favorites.size(), userId);
            log.info("favourite {}" ,favorites);
            return favorites;
        } catch (Exception e) {
            log.error("Failed to fetch favorite places for user ID: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }

    public void addFavoritePlace(String userId, String placeId) {
        log.info("Attempting to add favorite place {} for user ID: {}", placeId, userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getPreferences() == null) {
                user.setPreferences(User.UserPreferences.builder()
                        .favoritePlaceIds(new ArrayList<>())
                        .build());
                log.debug("Initializing preferences for user {}", userId);
            }

            if (user.getPreferences().getFavoritePlaceIds() == null) {
                user.getPreferences().setFavoritePlaceIds(new ArrayList<>());
                log.debug("Initializing favorite place IDs list for user {}", userId);
            }

            if (!user.getPreferences().getFavoritePlaceIds().contains(placeId)) {
                user.getPreferences().getFavoritePlaceIds().add(placeId);
                userRepository.save(user);
                log.info("Successfully added place {} to favorites for user {}", placeId, userId);
            } else {
                log.info("Place {} is already a favorite for user {}. No action taken.", placeId, userId);
            }
        } catch (Exception e) {
            log.error("Failed to add favorite place {} for user {}. Error: {}", placeId, userId, e.getMessage());
            throw e;
        }
    }

    public void removeFavoritePlace(String userId, String placeId) {
        log.info("Attempting to remove favorite place {} for user ID: {}", placeId, userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getPreferences() != null && user.getPreferences().getFavoritePlaceIds() != null) {
                boolean removed = user.getPreferences().getFavoritePlaceIds().remove(placeId);
                if (removed) {
                    userRepository.save(user);
                    log.info("Successfully removed place {} from favorites for user {}", placeId, userId);
                } else {
                    log.warn("Place {} was not found in favorites for user {}. No action taken.", placeId, userId);
                }
            } else {
                log.warn("User {} has no favorite places to remove from.", userId);
            }
        } catch (Exception e) {
            log.error("Failed to remove favorite place {} for user {}. Error: {}", placeId, userId, e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> getUserTokenHistory(String userId, int days) {
        log.info("Fetching token history for last {} days for user: {}", days, userId);
        LocalDateTime start = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);

        List<Queue> allQueues = queueRepository.findAll(); // inject QueueRepository

        Map<LocalDate, Long> dailyCounts = allQueues.stream()
                .flatMap(q -> q.getTokens().stream())
                .filter(t -> userId.equals(t.getUserId()))
                .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()))
                .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(start))
                .collect(Collectors.groupingBy(
                        t -> t.getCompletedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            counts.add(dailyCounts.getOrDefault(date, 0L));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }

// In UserService.java

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public void addFcmToken(String userId, String fcmToken) {
        User user = getUserOrThrow(userId);
        if (user.getFcmTokens() == null) {
            user.setFcmTokens(new ArrayList<>());
        }
        if (!user.getFcmTokens().contains(fcmToken)) {
            user.getFcmTokens().add(fcmToken);
            userRepository.save(user);
            log.info("FCM token added for user {}", userId);
        }
    }

    public void removeFcmToken(String userId, String fcmToken) {
        User user = getUserOrThrow(userId);
        if (user.getFcmTokens() != null) {
            user.getFcmTokens().remove(fcmToken);
            userRepository.save(user);
            log.info("FCM token removed for user {}", userId);
        }
    }

    public List<UserTokenHistoryDTO> getUserTokenHistoryOptimized(String userId, int days, Pageable pageable) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // Aggregation pipeline
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.unwind("tokens"),
                Aggregation.match(Criteria.where("tokens.userId").is(userId)
                        .and("tokens.issuedAt").gte(cutoff)
                        .and("tokens.status").in(TokenStatus.COMPLETED.toString(), TokenStatus.CANCELLED.toString())),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "tokens.issuedAt")),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize()),
                Aggregation.project()
                        .and("tokens.tokenId").as("tokenId")
                        .and("_id").as("queueId")
                        .and("serviceName").as("serviceName")
                        .and("placeId").as("placeId")
                        .and("tokens.status").as("status")
                        .and("tokens.issuedAt").as("issuedAt")
                        .and("tokens.servedAt").as("servedAt")
                        .and("tokens.completedAt").as("completedAt")
                        .and("tokens.serviceDurationMinutes").as("serviceDurationMinutes")
        );

        AggregationResults<UserTokenHistoryDTO> results = mongoTemplate.aggregate(agg, "queues", UserTokenHistoryDTO.class);
        List<UserTokenHistoryDTO> history = results.getMappedResults();

        if (history.isEmpty()) {
            return history;
        }

        // Batch load place names
        Set<String> placeIds = history.stream()
                .map(UserTokenHistoryDTO::getPlaceId)
                .collect(Collectors.toSet());
        Map<String, String> placeNames = placeService.getPlacesByIds(new ArrayList<>(placeIds)).stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));

        // Batch load ratings
        Set<String> tokenIds = history.stream()
                .map(UserTokenHistoryDTO::getTokenId)
                .collect(Collectors.toSet());
        Map<String, Integer> ratings = feedbackRepository.findByTokenIdIn(tokenIds).stream()
                .collect(Collectors.toMap(Feedback::getTokenId, Feedback::getRating));

        // Enrich DTOs
        history.forEach(dto -> {
            dto.setPlaceName(placeNames.get(dto.getPlaceId()));
            dto.setRating(ratings.get(dto.getTokenId()));

            // Calculate wait time minutes if needed
            if (dto.getServedAt() != null && dto.getIssuedAt() != null) {
                dto.setWaitTimeMinutes(Duration.between(dto.getIssuedAt(), dto.getServedAt()).toMinutes());
            }
        });

        return history;
    }

    public void updateProfileImage(String userId, String imageUrl) {
        User user = getUserOrThrow(userId);
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);
        log.info("Profile image updated for user: {}", userId);
    }

}