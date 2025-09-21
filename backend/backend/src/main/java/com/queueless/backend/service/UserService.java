package com.queueless.backend.service;

import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.UserProfileUpdateRequest;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}