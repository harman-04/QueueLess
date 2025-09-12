package com.queueless.backend.controller;

import com.queueless.backend.dto.PasswordChangeRequest;
import com.queueless.backend.dto.UserProfileUpdateRequest;
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

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Helper method to get the authenticated user's ID
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }
        return authentication.getName();
    }

    /**
     * Endpoint to update user profile information, including a direct image URL.
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        String userId = getCurrentUserId();
        log.info("API call: PUT /api/user/profile for userId: {}", userId);
        try {
            userService.updateUserProfile(userId, request);
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
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        String userId = getCurrentUserId();
        log.info("API call: PUT /api/user/password for userId: {}", userId);
        try {
            userService.changePassword(userId, request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            log.error("Failed to change password for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Endpoint to disable or delete the user's account.
     */
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        String userId = getCurrentUserId();
        log.info("API call: DELETE /api/user/account for userId: {}", userId);
        try {
            userService.deleteAccount(userId);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete account for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete account.");
        }
    }
}