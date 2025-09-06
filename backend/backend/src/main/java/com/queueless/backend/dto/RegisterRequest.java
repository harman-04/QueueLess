package com.queueless.backend.dto;

import com.queueless.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String phoneNumber;
    private Role role;
    private String token; // for admin registration token
    private String placeId; // for provider registration
    private UserPreferences preferences; // New field for user preferences

    @Data
    public static class UserPreferences {
        private Boolean emailNotifications = true;
        private Boolean smsNotifications = false;
        private String language = "en";
        private Integer defaultSearchRadius = 5;
        private Boolean darkMode = false;
    }
}